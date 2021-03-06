/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qa.qcri.aidr.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
//import java.util.logging.Level;
//import java.util.logging.Logger;



import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
//import org.supercsv.cellprocessor.constraint.UniqueHashCode;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.encoder.DefaultCsvEncoder;
import org.supercsv.exception.SuperCsvCellProcessorException;

import qa.qcri.aidr.logging.ErrorLog;
import qa.qcri.aidr.utils.Config;
import qa.qcri.aidr.utils.Tweet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import qa.qcri.aidr.utils.ClassifiedTweet;

/**
 *
 * @author Imran
 */
public class ReadWriteCSV {
	
	//private static final int BUFFER_SIZE = 10 * 1024 * 1024;
	private static Logger logger = Logger.getLogger(ReadWriteCSV.class.getName());
	private static ErrorLog elog = new ErrorLog();
	
	private static CellProcessor[] getProcessors4TweetIDSCCSV() {

		final CellProcessor[] processors = new CellProcessor[]{
				//new UniqueHashCode(), // tweetID (must be unique)
				new NotNull(),	// tweetID (must be unique)
		};


		return processors;
	}
	
	private static CellProcessor[] getProcessors() {

		final CellProcessor[] processors = new CellProcessor[]{
				//new UniqueHashCode(), // tweetID (must be unique)
				//new NotNull(),	// tweetID (must be unique)
				new Optional(),		// data shows that sometimes tweetID CAN be null!
				new Optional(), // message
				//new FmtDate("dd/MM/yyyy"), // birthDate
				new Optional(), // userID
				new Optional(), // userName
				//new Optional(new FmtBool("Y", "N")), // isRT
				new Optional(), // userURL
				new Optional(), // createdAt
				new Optional(), // tweet permanent URL

		};


		return processors;
	}

	private static CellProcessor[] getProcessors4ClassifiedCCSV() {

		final CellProcessor[] processors = new CellProcessor[]{
				//new UniqueHashCode(), // tweetID (must be unique)
				//new NotNull(),	// tweetID (must be unique)
				new Optional(),		// data shows that sometimes tweetID CAN be null!
				new Optional(), // message
				new Optional(), // userID
				new Optional(), // userName
				new Optional(), // userURL
				new Optional(), // createdAt
				new Optional(), // tweet permanent URL
				new Optional(), // tweet permanent URL
				new Optional(), // tweet permanent URL
				new Optional(), // tweet permanent URL
				new Optional(), // tweet permanent URL
				new Optional(), // tweet permanent URL
		};
		return processors;
	}

	private static CellProcessor[] getProcessors4ClassifiedTweetIDSCCSV() {

		final CellProcessor[] processors = new CellProcessor[]{
				//new UniqueHashCode(), // tweetID (must be unique)
				new NotNull(),	// tweetID (must be unique): sometimes CAN be null!
				new Optional(), // labelname
				new Optional(), // labeldescription
				new Optional(), // confidence
		};
		return processors;
	}

	public ICsvBeanWriter getCSVBeanWriter(String fileToWrite) {
		try {
			return new CsvBeanWriter(new FileWriter(fileToWrite, true),
					new CsvPreference.Builder(CsvPreference.EXCEL_PREFERENCE)
			.useEncoder(new DefaultCsvEncoder())
			.build() );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("Error in creating CSV Bean writer!");
			logger.error(elog.toStringException(e));
		}
		return null;
	}

	public ICsvBeanWriter writeCollectorTweetIDSCSV(ICsvBeanWriter beanWriter, List<Tweet> tweetsList, String collectionDIR, String fileName) {
		List<Tweet> twtList = tweetsList;
		//ICsvBeanWriter beanWriter = null;
		try {
			// the header elements are used to map the bean values to each column (names must match)
			//final String[] header = new String[]{"tweetID", "message","userID", "userName", "userURL", "createdAt", "tweetURL"};
			//final CellProcessor[] processors = getProcessors();
			
			// koushik: shouldn't we be writing only the tweetIDs?
			final String[] header = new String[]{"tweetID"};
			
			final CellProcessor[] processors = getProcessors4TweetIDSCCSV();
			
			String persisterDIR = Config.DEFAULT_PERSISTER_FILE_PATH;
			fileName = StringUtils.substringBefore(fileName, ".json"); //removing .json extension
			String fileToWrite = persisterDIR + collectionDIR + "/" + fileName + ".csv";
			logger.error(collectionDIR + ": Writing CSV file : " + fileToWrite);
			//beanWriter = new CsvBeanWriter(new FileWriter(fileToWrite, true),
			//        CsvPreference.EXCEL_PREFERENCE);
			if (null == beanWriter) { 
				beanWriter = getCSVBeanWriter(fileToWrite);
				// write the header
				beanWriter.writeHeader(header);
			}

			for (final Tweet twt : twtList) {
				try {
					if (twt.getTweetID() != null) {
						beanWriter.write(twt, header, processors);
					}
				} catch (SuperCsvCellProcessorException e) {
					//Logger.getLogger(ReadWriteCSV.class.getName()).log(Level.SEVERE, "[writeCollectorTweetIDSCSV] SuperCSV error");
					logger.error(collectionDIR + ": SuperCSV error");
					//e.printStackTrace();
				}
			}

		} catch (IOException ex) {
			//Logger.getLogger(ReadWriteCSV.class.getName()).log(Level.SEVERE, "[writeCollectorTweetIDSCSV] IO Exception occured");
			logger.error(collectionDIR + ": IO Exception occured");
			//ex.printStackTrace();
		} 
		//return fileName+".csv";
		return beanWriter;
	}

	public ICsvBeanWriter writeClassifiedTweetIDsCSV(ICsvBeanWriter beanWriter, final List<ClassifiedTweet> tweetsList, String collectionDIR, String fileName) {
		//List<ClassifiedTweet> twtList = tweetsList;//new ArrayList<Tweet>(); 

		//ICsvBeanWriter beanWriter = null;
		try {
			// the header elements are used to map the bean values to each column (names must match)
			final String[] header = new String[]{"tweetID", "labelName","labelDescription", "confidence"};
			final CellProcessor[] processors = getProcessors4ClassifiedTweetIDSCCSV();

			String persisterDIR = Config.DEFAULT_PERSISTER_FILE_PATH;
			fileName = StringUtils.substringBefore(fileName, ".json"); //removing .json extension
			String fileToWrite = persisterDIR + collectionDIR + "/output/" + fileName + ".csv";
			logger.error(collectionDIR + ": Writing CSV file : " + fileToWrite);
			//beanWriter = new CsvBeanWriter(new FileWriter(fileToWrite, true),
			//        CsvPreference.EXCEL_PREFERENCE);
			if (null == beanWriter) {
				beanWriter = getCSVBeanWriter(fileToWrite);
				// write the header
				beanWriter.writeHeader(header);
			} 

			for (final ClassifiedTweet twt : tweetsList) {
				try {
					if (twt.getTweetID() != null) {
						beanWriter.write(twt, header, processors);
					}
				} catch (SuperCsvCellProcessorException e) {
					logger.error(collectionDIR + ": SuperCSV error");
					//e.printStackTrace();
				}
			}

		} catch (IOException ex) {
			logger.error(collectionDIR + ": IO Exception occured");
			//ex.printStackTrace();

		} 
		//return fileName+".csv";
		return beanWriter;
	}

	public ICsvBeanWriter writeCollectorTweetsCSV(List<Tweet> tweetsList, String collectionDIR, String fileName, ICsvBeanWriter beanWriter) {
		List<Tweet> twtList = tweetsList;//new ArrayList<Tweet>();
		try {
			final String[] header = new String[]{"tweetID", "message","userID", "userName", "userURL", "createdAt", "tweetURL"};
			final CellProcessor[] processors = getProcessors();

			if(null == beanWriter){
				String persisterDIR = Config.DEFAULT_PERSISTER_FILE_PATH;
				fileName = StringUtils.substringBefore(fileName, ".json"); //removing .json extension
				String fileToWrite = persisterDIR + collectionDIR + "/" + fileName + ".csv";
				logger.info(collectionDIR + ": Writing CSV file : " + fileToWrite);
				//beanWriter = new CsvBeanWriter(new FileWriter(fileToWrite, true),
				//        CsvPreference.EXCEL_PREFERENCE);
				beanWriter = getCSVBeanWriter(fileToWrite);
				beanWriter.writeHeader(header);
			}

			for (final Tweet twt : twtList) {
				try {
					beanWriter.write(twt, header, processors);
				} catch (SuperCsvCellProcessorException e) {
					logger.error(collectionDIR + ": SuperCSV error");
					//e.printStackTrace();
				}
			}

		} catch (IOException ex) {
			logger.error(collectionDIR + ": IO Exception occured");
			//ex.printStackTrace();
		}
		return beanWriter;
	}


	public ICsvBeanWriter writeClassifiedTweetsCSV(List<ClassifiedTweet> tweetsList, String collectionDIR, String fileName, ICsvBeanWriter beanWriter) {
		List<ClassifiedTweet> twtList = tweetsList;//new ArrayList<Tweet>();
		try {
			final String[] header = new String[]{"tweetID", "message","userID", "userName", "userURL", "createdAt", "tweetURL", "crisisName", "labelName", "labelDescription", "confidence", "humanLabeled"};
			final CellProcessor[] processors = getProcessors4ClassifiedCCSV();

			if(null == beanWriter){
				String persisterDIR = Config.DEFAULT_PERSISTER_FILE_PATH;
				fileName = StringUtils.substringBefore(fileName, ".json"); //removing .json extension
				String fileToWrite = persisterDIR + collectionDIR + "/output/" + fileName + ".csv";
				logger.info(collectionDIR + ": Writing CSV file : " + fileToWrite);
				//beanWriter = new CsvBeanWriter(new FileWriter(fileToWrite, true),
				//        CsvPreference.EXCEL_PREFERENCE);
				beanWriter = getCSVBeanWriter(fileToWrite);
				beanWriter.writeHeader(header);
			}

			for (final ClassifiedTweet twt : twtList) {
				try {
					//System.out.println("To WRITE TWEET: " + twt);
					beanWriter.write(twt, header, processors);
				} catch (SuperCsvCellProcessorException e) {
					logger.error(collectionDIR + ": SuperCSV error. Offending tweet: " + twt.getMessage());
					//e.printStackTrace();
				}
			}

		} catch (IOException ex) {
			logger.error(collectionDIR + ": IO Exception occured");
			//ex.printStackTrace();
		}
		return beanWriter;
	}

	/*
	private final static String getDateTime() {
		DateFormat df = new SimpleDateFormat("yyyyMMdd");  //yyyy-MM-dd_hh:mm:ss
		//df.setTimeZone(TimeZone.getTimeZone("PST"));  
		return df.format(new Date());
	}
	*/
}
