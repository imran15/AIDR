package qa.qcri.aidr.manager.service;

import qa.qcri.aidr.manager.dto.*;
import qa.qcri.aidr.manager.exception.AidrException;
import qa.qcri.aidr.manager.hibernateEntities.AidrCollection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TaggerService {

    public List<TaggerCrisisType> getAllCrisisTypes() throws AidrException;

    public List<TaggerCrisis> getCrisesByUserId(Integer userId) throws AidrException;

    public String createNewCrises(TaggerCrisisRequest crisis) throws AidrException;

    public Collection<TaggerAttribute> getAttributesForCrises(Integer crisisID, Integer userId) throws AidrException;

    public TaggerCrisisExist isCrisesExist(String code) throws AidrException;

    public Integer isUserExistsByUsername(String userName) throws AidrException;

    public Integer addNewUser(TaggerUser taggerUser) throws AidrException;

    public Integer addAttributeToCrisis(TaggerModelFamily modelFamily) throws AidrException;

    public TaggerCrisis getCrisesByCode(String code) throws AidrException;

    public TaggerCrisis updateCode(TaggerCrisis crisis) throws AidrException;

    public List<TaggerModel> getModelsForCrisis(Integer crisisID) throws AidrException;

    public List<TaggerModelNominalLabel> getAllLabelsForModel(Integer modelID) throws AidrException;

    public TaggerAttribute createNewAttribute(TaggerAttribute attribute) throws AidrException;

    public TaggerAttribute getAttributeInfo(Integer id) throws AidrException;

    public TaggerLabel getLabelInfo(Integer id) throws AidrException;

    public boolean deleteAttribute(Integer id) throws AidrException;

    public boolean deleteTrainingExample(Integer id) throws AidrException;

    public boolean removeAttributeFromCrises(Integer modelFamilyID) throws AidrException;

    public TaggerAttribute updateAttribute(TaggerAttribute attribute) throws AidrException;

    public TaggerLabel updateLabel(TaggerLabelRequest label) throws AidrException;

    public TaggerLabel createNewLabel(TaggerLabelRequest label) throws AidrException;

    public TaggerAttribute attributeExists(String code) throws AidrException;

    public List<TrainingDataDTO> getTrainingDataByModelIdAndCrisisId(Integer modelFamilyId,
                                                                     Integer crisisId,
                                                                     Integer start,
                                                                     Integer limit,
                                                                     String sortColumn,
                                                                     String sortDirection) throws AidrException;

    public String getAssignableTask(Integer id, String userName)  throws AidrException;

    public String getTemplateStatus(String code)  throws AidrException;

    public String skipTask(Integer id, String userName)  throws AidrException;

    public boolean saveTaskAnswer(List<TaskAnswer> taskAnswer) throws AidrException;

    public String loadLatestTweets(String code, String constraints) throws Exception;

    public ModelHistoryWrapper getModelHistoryByModelFamilyID(Integer start, Integer limit, Integer id) throws Exception;

    public Map<String, Integer> getTaggersForCollections(List<String> collectionCodes) throws Exception;

    public boolean pingTagger() throws AidrException;

    public boolean pingTrainer() throws AidrException;

    public boolean pingAIDROutput() throws AidrException;

    public String getRetainingThreshold() throws AidrException;

    public String getAttributesAndLabelsByCrisisId(Integer id) throws Exception;

    // Added by koushik
	public String generateCSVLink(String code) throws AidrException;

	// Added by koushik
	public String generateTweetIdsLink(String code) throws AidrException;

	//Added by koushik
	public int trashCollection(AidrCollection collection) throws Exception;
	
	//Added by koushik
	public int untrashCollection(String collectionCode) throws Exception;

    public String loadLatestTweetsWithCount(String code, int count) throws AidrException;

    //Added by koushik
	public String generateJSONLink(String code) throws AidrException;

	//Added by koushik
	public String generateJsonTweetIdsLink(String code) throws AidrException;

}
