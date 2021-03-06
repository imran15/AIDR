/** 
 * @author Koushik Sinha
 * Last modified: 06/01/2014
 * 
 * The ChannelBufferManager class implements the following channel buffer management functionalities 
 * for the /getLast REST API: 
 * 		a) Subscribe to all CHANNEL_PREFIX_STRING channels in REDIS using pattern-based subscription.
 * 		b) Monitor the REDIS pubsub system for new or removed channels.
 * 		c) If new channel found, then create a new ChannelBuffer for it.
 * 		d) If no messages received on an existing channel for a certain duration then delete the channel.
 * 		e) Return an ArrayList<String> of messages for a specific channel and messageCount value. 
 *
 */

package qa.qcri.aidr.output.getdata;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import qa.qcri.aidr.output.entity.AidrCollection;
import qa.qcri.aidr.output.filter.ClassifiedFilteredTweet;
import qa.qcri.aidr.output.utils.AIDROutputConfig;
import qa.qcri.aidr.output.utils.DatabaseController;
import qa.qcri.aidr.output.utils.DatabaseInterface;
import qa.qcri.aidr.output.utils.ErrorLog;
import qa.qcri.aidr.output.utils.JedisConnectionObject;
import qa.qcri.aidr.output.utils.QuickSort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

//import org.apache.log4j.BasicConfigurator;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

public class ChannelBufferManager {

	private static final int NO_DATA_TIMEOUT = 48 * 60 * 60 * 1000;		// when to delete a channel buffer
	private static final int CHECK_INTERVAL = NO_DATA_TIMEOUT;

	private static Logger logger = Logger.getLogger(ChannelBufferManager.class);
	private static ErrorLog elog = new ErrorLog();

	// Thread related
	private static ExecutorService executorServicePool = null;

	// Redis connection related
	public static String redisHost = "localhost";	// Current assumption: REDIS running on same m/c
	public static int redisPort = 6379;	

	// Jedis related
	public static JedisConnectionObject jedisConn = null;		// we need only a single instance of JedisConnectionObject running in background
	public Jedis subscriberJedis = null;
	public RedisSubscriber aidrSubscriber = null;

	// Runtime related
	private boolean isConnected = false;
	private boolean isSubscribed =false;
	private long lastCheckedTime = 0; 
	private int bufferSize = -1;

	// Channel Buffering Algorithm related
	private final String CHANNEL_PREFIX_STRING = "aidr_predict.";
	public static ConcurrentHashMap<String, ChannelBuffer> subscribedChannels;

	// DB access related
	private static DatabaseInterface dbController = null;
	private String managerMainUrl = "http://localhost:8080/AIDRFetchManager";

	//////////////////////////////////////////
	// ********* Method definitions *********
	//////////////////////////////////////////

	// Constructor
	public ChannelBufferManager(final String channelRegEx) {
		AIDROutputConfig configuration = new AIDROutputConfig();
		HashMap<String, String> configParams = configuration.getConfigProperties();

		redisHost = configParams.get("host");
		redisPort = Integer.parseInt(configParams.get("port"));
		if (configParams.get("logger").equalsIgnoreCase("log4j")) {
			// For now: set up a simple configuration that logs on the console
			// PropertyConfigurator.configure("log4j.properties");      
			//BasicConfigurator.configure();    // initialize log4j logging
		}
		if (configParams.get("logger").equalsIgnoreCase("slf4j")) {
			System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");	// set logging level for slf4j
		}
		logger.info("Initializing channel buffer manager.");
		System.out.println("[ChannelBufferManager] Initializing channel buffer manager.");

		bufferSize = -1;
		executorServicePool = Executors.newCachedThreadPool();	//Executors.newFixedThreadPool(10);		// max number of threads
		logger.info("Created thread pool: " + executorServicePool);

		jedisConn = new JedisConnectionObject(redisHost, redisPort);
		try {
			subscriberJedis = jedisConn.getJedisResource();
			if (subscriberJedis != null) isConnected = true;
		} catch (JedisConnectionException e) {
			subscriberJedis = null;
			isConnected = false;
			logger.error("Fatal error! Couldn't establish connection to REDIS!");
			e.printStackTrace();
			//System.exit(1);
		}
		if (isConnected) {
			aidrSubscriber = new RedisSubscriber();
			jedisConn.setJedisSubscription(subscriberJedis, true);		// we will be using pattern-based subscription
			logger.info("Created new Jedis connection: " + subscriberJedis);
			try {
				subscribeToChannel(channelRegEx);
				//this.channelRegEx = channelRegEx;
				isSubscribed = true;
				logger.info("Created pattern subscription");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				isSubscribed = false;
				logger.error("Fatal exception occurred attempting subscription: " + e.toString());
				logger.error(elog.toStringException(e));
				//System.exit(1);
			}
			if (isSubscribed) {
				subscribedChannels = new ConcurrentHashMap<String,ChannelBuffer>();
				logger.debug("Created HashMap");
			}
		}

		try {
			dbController = new DatabaseController();
			logger.info("Created dbController = " + dbController);
		} catch (Exception e) {
			logger.error("Couldn't initiate DB access to aidr_fetch_manager");
			logger.error("[ChannelBufferManager] Couldn't initiate DB access to aidr_fetch_manager");
			logger.error(elog.toStringException(e));
		}

	}

	public ChannelBufferManager(final int bufferSize, final String channelRegEx) {
		this(channelRegEx);					// call default constructor
		this.bufferSize = bufferSize;		// set buffer size
	}


	// Does all the essential work:
	// 1. Searches received message to see if channel name present.
	// 2. If channel present then simply adds receivedMessage to that channel.
	// 3. Else, first calls createChannelBuffer() and then executes step (2).
	// 4. Deletes channelName and channel buffer if channelName not seen for TIMEOUT duration.
	private void manageChannelBuffers(final String subscriptionPattern, 
			final String channelName, 
			final String receivedMessage) {
		if (null == channelName) {
			logger.error("Something terribly wrong! Fatal error in: " + channelName);
			//System.exit(1);
		}
		if (isChannelPresent(channelName)) {
			// Add to appropriate circular buffer
			addMessageToChannelBuffer(channelName, receivedMessage);
		}
		else {
			//First create a new circular buffer and then add to that buffer
			createChannelQueue(channelName);
			addMessageToChannelBuffer(channelName, receivedMessage);
			logger.info("Created new channel: " + channelName);
			System.out.println("[manageChannelBuffers] Created new channel: " + channelName);
		}
		// Periodically check if any channel is down - if so, delete
		long currentTime = new Date().getTime();
		if (currentTime - lastCheckedTime > CHECK_INTERVAL) {
			logger.info("Periodic check for inactive channels - delete if any.");
			List<ChannelBuffer>cbList = new ArrayList<ChannelBuffer>();
			cbList.addAll(subscribedChannels.values());
			Iterator<ChannelBuffer>it = cbList.iterator();
			while (it.hasNext()) {
				ChannelBuffer temp = it.next();
				if ((currentTime - temp.getLastAddTime()) > NO_DATA_TIMEOUT) {
					logger.info("Deleting inactive channel = " + channelName);
					deleteChannelBuffer(temp.getChannelName());
				}
			}
			lastCheckedTime = new Date().getTime();
			cbList.clear();
			cbList = null;
		}
	}

	public void addMessageToChannelBuffer(final String channelName, final String msg) {
		ChannelBuffer cb = subscribedChannels.get(channelName);
		cb.addMessage(msg);
		subscribedChannels.put(channelName, cb);
	}

	public List<String> getLastMessages(String channelName, int msgCount) {
		if (isChannelPresent(channelName)) {
			ChannelBuffer cb = subscribedChannels.get(channelName);
			// Note: Ideally, we should have used the method call cb.getMessages(msgCount)
			// However, we get all messages in buffer since we do not know how many will 
			// eventually be valid, due to rejectNullFlag setting in caller. The filtering
			// to send msgCount number of messages will happen in the caller. 
			return cb != null ? cb.getMessages(cb.getBufferSize()) : null;		
		}
		return null;
	}

	// Returns true if channelName present in list of channels
	// TODO: define the appropriate collections data structure - HashMap, HashSet, ArrayList? 
	public boolean isChannelPresent(String channelName) {
		return subscribedChannels.containsKey(channelName);
	}

	// channelName = fully qualified channel name as present in REDIS pubsub system
	public void createChannelQueue(final String channelName) {
		if (!isChannelPresent(channelName)) {
			ChannelBuffer cb = new ChannelBuffer(channelName);
			if (bufferSize <= 0)
				cb.createChannelBuffer();				// use default buffer size
			else
				cb.createChannelBuffer(bufferSize);		// use specified buffer size
			subscribedChannels.put(channelName, cb);
		}
		else {
			logger.error(channelName + ": Trying to create an existing channel! Should never be here!");
		}
		//isChannelPublic(channelName);
	}

	public void deleteChannelBuffer(final String channelName) {
		if (isChannelPresent(channelName)) {
			ChannelBuffer cb = subscribedChannels.get(channelName);
			cb.deleteBuffer();
			subscribedChannels.remove(channelName);
			logger.info("Deleted channel buffer: " + channelName);
		}
	}

	public void deleteAllChannelBuffers() {
		if (subscribedChannels != null) {
			logger.info("Deleting buffers for currently subscribed list of channels: " + subscribedChannels.keySet());
			for (String channelId: subscribedChannels.keySet()) {
				subscribedChannels.get(channelId).deleteBuffer();
				subscribedChannels.remove(channelId);
			}
			subscribedChannels.clear();
		}
	}

	/** 
	 * @return A set of fully qualified channel names, null if none found
	 */
	public Set<String> getActiveChannelsList() {
		final Set<String> channelSet = new HashSet<String>();
		channelSet.addAll(subscribedChannels.keySet().isEmpty() 
				? new HashSet<String>() : subscribedChannels.keySet());
		return channelSet.isEmpty() ? null : channelSet;
	}

	/** 
	 * @return A set of only the channel codes - stripped of CHANNEL_PREFIX_STRING, null if none found
	 */
	public Set<String> getActiveChannelCodes() {
		final Set<String> channelCodeSet = new HashSet<String>();
		Set<String> tempSet = getActiveChannelsList();
		for (String s:tempSet) {
			channelCodeSet.add(s.substring(CHANNEL_PREFIX_STRING.length()));
		}
		tempSet.clear();
		tempSet = null;
		return channelCodeSet.isEmpty() ? null : channelCodeSet;
	}

	/**
	 * 
	 * @param channelName
	 * @return true if channel is publicly listed, false otherwise
	 */

	private boolean isChannelPublic(String channelName) {
		//logger.info("[isChannelPublic] Received request for channel: " + channelName);
		//first strip off the prefix aidr_predict.
		String channelCode = channelName;
		if (channelName.startsWith(CHANNEL_PREFIX_STRING)) {
			String[] strs = channelName.split(CHANNEL_PREFIX_STRING);
			channelCode = (strs != null) ? strs[1] : channelName;
		}
		//logger.info("[isChannelPublic] Querying for: " + channelCode);
		Criterion criterion = Restrictions.eq("code", channelCode);
		AidrCollection collection = dbController.getByCriteria(criterion);
		if (collection != null) {
			//logger.info("channel: " + channelName + ", code = " + collection.getCode() + ", public = " + collection.getPubliclyListed());
			return collection.getPubliclyListed();
		} else {
			//logger.info("channel: " + channelName + ", fetched collection = " + collection);
		}
		return false;
	}

	/**
	 * Used in conjunction with getAllRunningCollections()
	 * @param channelName: channel code of channel to test
	 * @param collectionList: list of all public channels returned by getAllRunningCollections()
	 * @return true if public, false otherwise
	 */
	private boolean isChannelPublic(String channelName, Map<String, Boolean> collectionList) {
		//logger.info("[isChannelPublic] Received request for channel: " + channelName);

		//first strip off the prefix aidr_predict.
		String channelCode = channelName;
		if (channelName.startsWith(CHANNEL_PREFIX_STRING)) {
			String[] strs = channelName.split(CHANNEL_PREFIX_STRING);
			channelCode = (strs != null) ? strs[1] : channelName;
		}
		if (collectionList != null) {
			if (collectionList.containsKey(channelCode)) {
				//logger.info("channel: " + channelName + ", code = " + channelCode + ", public = " + collectionList.get(channelCode));
				return collectionList.get(channelCode);
			}
		} else {
			logger.error("[isChannelPublic] collection list is null !!! Returning false");
		}
		return false;
	}

	/**
	 * Currently unused - since earlier there were errors in the manager's
	 * public collection REST API.
	 * @return
	 */
	public Map<String, Boolean> getAllRunningCollections() {
		Map<String, Boolean> collectionList = null;
		Client client = ClientBuilder.newBuilder().register(JacksonFeature.class).build();
		try {
			//logger.debug("Request received");
			WebTarget webResource = client.target(managerMainUrl 
					+ "/public/collection/findAllRunning.action?start=0&limit=10000");

			Response clientResponse = webResource.request(MediaType.APPLICATION_JSON).get();

			String jsonResponse = clientResponse.readEntity(String.class);
			//logger.info(" Response received" + jsonResponse);
			if (jsonResponse != null) {
				try {
					Gson jsonObject = new GsonBuilder().serializeNulls().disableHtmlEscaping()
							.serializeSpecialFloatingPointValues()	
							.create();

					JsonParser parser = new JsonParser();
					JsonObject obj = (JsonObject) parser.parse(jsonResponse);
					//logger.debug("Response parsed");

					JsonArray jsonData= null;
					if (obj != null && obj.has("data")) {			// if false, then something wrong in AIDR setup
						jsonData = obj.get("data").getAsJsonArray();
						collectionList = new HashMap<String, Boolean>();
						for (int i = 0;i < jsonData.size();i++) {
							JsonObject collection = (JsonObject) jsonData.get(i);
							String collectionCode = collection.get("code").getAsString();
							Boolean status = collection.get("publiclyListed").getAsBoolean();
							collectionList.put(collectionCode, status);
							//logger.debug("Retrieved collection: code = " + collectionCode + ", status = " + status);
						}
					} else {
						logger.error("Error in received response from AIDRFetchManager: " + jsonData);
					}
				} catch (Exception e) {
					logger.error("Error in parsing received resposne from manager: " + jsonResponse);
					logger.error(elog.toStringException(e));
				}
			}
		} catch (Exception e) {
			logger.error("Error in making REST call");
			logger.error(elog.toStringException(e));
		}
		return collectionList;
	}


	/**
	 * @return List of latest tweets seen on all active channels, one tweet/channel, null if none found
	 */
	public List<String> getLatestFromAllChannels(final int msgCount, final float confidenceThreshold) {
		final List<ChannelBuffer>cbList = new ArrayList<ChannelBuffer>();
		List<String>dataSet = new ArrayList<String>();

		// First get a list of all running collections from aidr-manager through REST call
		//Map<String, Boolean> collectionList = getAllRunningCollections();
		final int EXTRA = 3;		
		cbList.addAll(subscribedChannels.values());
		int k = -1;
		for (ChannelBuffer temp: cbList) {
			//logger.debug("cbList size = " + cbList.size() + ", Channel buffer: " + temp.getChannelName() + "isPublic = " + isChannelPublic(temp.getChannelName()));
			if (isChannelPublic(temp.getChannelName())) {
				final List<String> tempList = temp.getLIFOMessages(msgCount+EXTRA);		// reverse-chronologically ordered list
				if (!tempList.isEmpty()) {
					int channelFetchSize = Math.min(msgCount+EXTRA,tempList.size());
					for (int i = 0;i < channelFetchSize;i++) {
						//logger.info("TWEET: " + tempList.get(i));
						ClassifiedFilteredTweet tweet = new ClassifiedFilteredTweet().deserialize(tempList.get(i));
						//logger.info("Channel: " + tweet.getCrisisCode() + ", time: " + tweet.getCreatedAt() + ", conf=" + tweet.getMaxConfidence());
						if (tweet != null) {
							long tweetTime = tweet.getCreatedAt().getTime();
							if (k < 0) {
								if (tweet.getMaxConfidence() >= confidenceThreshold) {
									dataSet.add(k+1, tempList.get(i));
									++k;
									//logger.info("Added the very first tweet from channel: " + tweet.getCrisisCode());
								}
							}
							else {
								// get the last stored tweet in the dataSet
								ClassifiedFilteredTweet lastStoredTweet = new ClassifiedFilteredTweet().deserialize(dataSet.get(k));

								// rule 1: if more recent, include
								if (lastStoredTweet != null && lastStoredTweet.getCreatedAt().getTime() < tweetTime 
										&& tweet.getMaxConfidence() >= confidenceThreshold) {
									dataSet.add(k+1, tempList.get(i));
									++k;
									//logger.info("Added using rule 1 from channel: " + tweet.getCrisisCode());
								}

								// rule 2: if not recent but from another channel, include
								if (lastStoredTweet != null && lastStoredTweet.getCreatedAt().getTime() >= tweetTime
										&& !lastStoredTweet.getCrisisCode().equals(tweet.getCrisisCode())
										&& tweet.getMaxConfidence() >= confidenceThreshold) {
									String tempTweet = dataSet.remove(k);
									dataSet.add(k, tempList.get(i));
									dataSet.add(k+1,tempTweet);
									++k;
									//logger.info("Added using rule 2 from channel: " + tweet.getCrisisCode());
								}

								// rule 3: if timestamps are same for same crisis, include higher confidence
								if (lastStoredTweet != null && lastStoredTweet.getCreatedAt().getTime() == tweetTime
										&& lastStoredTweet.getCrisisCode().equals(tweet.getCrisisCode())
										&& lastStoredTweet.getMaxConfidence() < tweet.getMaxConfidence()) {
									dataSet.add(k+1, tempList.get(i));
									++k;
									//logger.info("Added using rule 3 from channel: " + tweet.getCrisisCode());
								}
							}
						}
					}
				}
			}	// end if (isChannelPublic)
		}	// end for
		// Now sort the dataSet in ascending order of timestamp
		QuickSort sorter = new QuickSort();
		sorter.sort(dataSet);
		for (int i = 0;i < dataSet.size();i++) {
			ClassifiedFilteredTweet tweet = new ClassifiedFilteredTweet().deserialize(dataSet.get(i));
			//logger.debug("timestamp = " + tweet.getCreatedAt().getTime());
		}
		return dataSet;
	}



	public Date extractTweetTimestampField(String tweet) {
		String offsetString = "\"created_at\":\"";
		StringBuffer strBuff = new StringBuffer();
		strBuff.append(tweet.substring(tweet.indexOf(offsetString) + offsetString.length(), tweet.indexOf("\",", tweet.indexOf(offsetString))));

		// Tweet date format: Tue Feb 18 08:46:03 +0000 2014
		SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZ yyyy");
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			return formatter.parse(strBuff.toString());
		} catch (ParseException e) {
			logger.error("Error in parsing string for timestamp :\n " + tweet);
			logger.error(elog.toStringException(e));
		}
		return null;
	}


	private void subscribeToChannel(final String channelRegEx) throws Exception {
		Future redisThread = executorServicePool.submit(new Runnable() {
			public void run() {
				Thread.currentThread().setName("ChannelBufferManager Redis subscription Thread");
				logger.info("New thread <" +  Thread.currentThread().getName() + "> created for subscribing to redis channel: " + channelRegEx);
				try {
					// Execute the blocking REDIS subscription call
					subscriberJedis.psubscribe(aidrSubscriber, channelRegEx);
				} catch (Exception e) {
					logger.error("AIDR Predict Channel pSubscribing failed for channel = " + channelRegEx);
					System.out.println("[subscribeToChannel] AIDR Predict Channel pSubscribing failed for channel = " + channelRegEx);
					stopSubscription();
					Thread.currentThread().interrupt();
				} finally {
					try {
						stopSubscription();
					} catch (Exception e) {
						logger.error(channelRegEx  + ": Exception occurred attempting stopSubscription: " + e.toString());
						logger.error(elog.toStringException(e));
					}
				}
				Thread.currentThread().interrupt();
				logger.info("Exiting thread: " + Thread.currentThread().getName());
			}
		});
	}

	private synchronized void stopSubscription() {
		try {
			if (aidrSubscriber != null && aidrSubscriber.getSubscribedChannels() > 0) {
				aidrSubscriber.punsubscribe();				
			}
		} catch (JedisConnectionException e) {
			logger.error("Connection to REDIS seems to be lost!");
		}
		try {
			if (jedisConn != null && aidrSubscriber != null) { 
				jedisConn.returnJedis(subscriberJedis);
				logger.info("Stopsubscription completed...");
				System.out.println("[stopSubscription] Stopsubscription completed...");
			}
		} catch (Exception e) {
			logger.error("Failed to return Jedis resource");
		}
		//this.notifyAll();
	}

	public void close() {
		stopSubscription();
		try {
			dbController.getEntityManager().close();
		} catch (IllegalStateException e) {
			logger.warn("attempting to close a container manager entitymanager");
		}
		//jedisConn.closeAll();
		deleteAllChannelBuffers();
		//executorServicePool.shutdown(); // Disable new tasks from being submitted
		shutdownAndAwaitTermination();
		logger.info("All done, fetch service has been shutdown...");
		System.out.println("[close] All done, fetch service has been shutdown...");
	}

	// cleanup all threads 
	void shutdownAndAwaitTermination() {
		int attempts = 0;
		executorServicePool.shutdown(); // Disable new tasks from being submitted
		while (!executorServicePool.isTerminated() && attempts < 10) {
			try {
				// Wait a while for existing tasks to terminate
				if (!executorServicePool.awaitTermination(30, TimeUnit.SECONDS)) {
					executorServicePool.shutdownNow();                         // Cancel currently executing tasks
					// Wait a while for tasks to respond to being cancelled
					if (!executorServicePool.awaitTermination(5, TimeUnit.SECONDS))
						logger.error("Executor Thread Pool did not terminate");
				} else {
					logger.info("All tasks completed post service shutdown");
				}
			} catch (InterruptedException e) {
				// (Re-)Cancel if current thread also interrupted
				executorServicePool.shutdownNow();
				// Preserve interrupt status
				Thread.currentThread().interrupt();
			} finally {
				executorServicePool.shutdownNow();
			}
			++attempts;
			if (!executorServicePool.isTerminated()) {
				logger.warn("Warning! Some threads not shutdown still. Trying again, attempt = " + attempts);
			}
		}
	}

	////////////////////////////////////////////////////
	private class RedisSubscriber extends JedisPubSub {

		@Override
		public void onMessage(String channel, String message) {}

		@Override
		public void onPMessage(String pattern, String channel, String message) {
			manageChannelBuffers(pattern, channel, message);
		}

		@Override
		public void onSubscribe(String channel, int subscribedChannels) {
			logger.info("Subscribed to channel:" + channel);
		}

		@Override
		public void onUnsubscribe(String channel, int subscribedChannels) {
			logger.info("Unsubscribed from channel:" + channel);
		}

		@Override
		public void onPUnsubscribe(String pattern, int subscribedChannels) {
			logger.info("Unsubscribed from channel pattern:" + pattern);
		}

		@Override
		public void onPSubscribe(String pattern, int subscribedChannels) {
			logger.info("Subscribed to channel pattern:" + pattern);
		}
	}

}
