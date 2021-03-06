package qa.qcri.aidr.predictui.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import qa.qcri.aidr.predictui.entities.Document;
import qa.qcri.aidr.predictui.entities.TaskAssignment;
import qa.qcri.aidr.predictui.entities.NominalLabel;






public class TaskManagerEntityMapper {

	public TaskManagerEntityMapper() {}

	public <E> E deSerializeList(String jsonString, TypeReference<E> type) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			if (jsonString != null) {
				E docList = mapper.readValue(jsonString, type);
				return docList;
			}	
		} catch (IOException e) {
			System.err.println("JSON deserialization exception");
			e.printStackTrace();
		}
		return null;
	}

	public <E> E deSerialize(String jsonString, Class<E> entityType) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			if (jsonString != null) {
				E entity = mapper.readValue(jsonString, entityType);
				return entity;
			}	
		} catch (IOException e) {
			System.err.println("JSON deserialization exception");
			e.printStackTrace();
		}
		return null;
	}

	public <E> String serializeTask(E task) {
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = null;
		try {
			if (task != null) jsonString = mapper.writeValueAsString(task);
		} catch (IOException e) {
			System.err.println("JSON serialization exception");
			e.printStackTrace();
		}
		return jsonString;
	}

	public Document transformDocument(qa.qcri.aidr.task.entities.Document document) {
		Document doc = new Document();
		if (document != null) {
			doc.setDocumentID(document.getDocumentID());
			doc.setCrisisID(document.getCrisisID());
			doc.setDoctype(document.getDoctype());
			doc.setData(document.getData());
			doc.setEvaluationSet(document.isEvaluationSet());
			doc.setGeoFeatures(document.getGeoFeatures());
			doc.setLanguage(document.getLanguage());
			doc.setHasHumanLabels(document.isHasHumanLabels());

			doc.setReceivedAt(document.getReceivedAt());
			doc.setSourceIP(document.getSourceIP());
			doc.setWordFeatures(document.getWordFeatures());
			doc.setValueAsTrainingSample(document.getValueAsTrainingSample());
			doc.setTaskAssignment(transformTaskAssignment(document.getTaskAssignment()));

			doc.setNominalLabelCollection(transformNominalLabelCollection(document.getNominalLabelCollection()));
			return doc;
		} 
		return null;
	}

	public qa.qcri.aidr.task.entities.Document reverseTransformDocument(Document document) {
		qa.qcri.aidr.task.entities.Document doc = new qa.qcri.aidr.task.entities.Document();
		if (document != null) {
			doc.setDocumentID(document.getDocumentID());
			doc.setCrisisID(document.getCrisisID());
			doc.setDoctype(document.getDoctype());
			doc.setData(document.getData());
			doc.setEvaluationSet(document.isEvaluationSet());
			doc.setGeoFeatures(document.getGeoFeatures());
			doc.setLanguage(document.getLanguage());
			doc.setHasHumanLabels(document.isHasHumanLabels());

			doc.setReceivedAt(document.getReceivedAt());
			doc.setSourceIP(document.getSourceIP());
			doc.setWordFeatures(document.getWordFeatures());
			doc.setValueAsTrainingSample(document.getValueAsTrainingSample());
			doc.setTaskAssignment(reverseTransformTaskAssignment(document.getTaskAssignment()));

			doc.setNominalLabelCollection(reverseTransformNominalLabelCollection(document.getNominalLabelCollection()));
			return doc;
		} 
		return null;
	}


	public TaskAssignment transformTaskAssignment(qa.qcri.aidr.task.entities.TaskAssignment t) {
		if (t != null) {
			TaskAssignment taskAssignment  = new TaskAssignment(t.getDocumentID(), t.getUserID(), t.getAssignedAt());
			return taskAssignment;
		}
		return null;
	}

	public qa.qcri.aidr.task.entities.TaskAssignment reverseTransformTaskAssignment(TaskAssignment t) {
		if (t != null) {
			qa.qcri.aidr.task.entities.TaskAssignment taskAssignment  = new qa.qcri.aidr.task.entities.TaskAssignment(t.getDocumentID(), t.getUserID(), t.getAssignedAt());
			return taskAssignment;
		}
		return null;
	}

	public Collection<NominalLabel> transformNominalLabelCollection(Collection<qa.qcri.aidr.task.entities.NominalLabel> list) {
		if (list != null) {
			Collection<NominalLabel> nominalLabelList = new ArrayList<NominalLabel>();
			for (qa.qcri.aidr.task.entities.NominalLabel t: list) {
				if (t != null) {
					NominalLabel nominalLabel  = new NominalLabel(t.getNominalLabelID(), t.getNominalLabelCode(), t.getName(), t.getDescription());
					nominalLabelList.add(nominalLabel);
				}
			}
			return nominalLabelList;
		}
		return null;
	}

	public Collection<qa.qcri.aidr.task.entities.NominalLabel> reverseTransformNominalLabelCollection(Collection<NominalLabel> list) {
		if (list != null) {
			Collection<qa.qcri.aidr.task.entities.NominalLabel> nominalLabelList = new ArrayList<qa.qcri.aidr.task.entities.NominalLabel>();
			for (NominalLabel t: list) {
				if (t != null) {
					qa.qcri.aidr.task.entities.NominalLabel nominalLabel = new qa.qcri.aidr.task.entities.NominalLabel(t.getNominalLabelID(), t.getNominalLabelCode(), t.getName(), t.getDescription());
					nominalLabelList.add(nominalLabel);
				}
			}
			return nominalLabelList;
		}
		return null;
	}



	public static void main(String args[]) {
		TaskManagerEntityMapper mapper = new TaskManagerEntityMapper();
		qa.qcri.aidr.task.entities.Document doc = new qa.qcri.aidr.task.entities.Document(12345678L, false);
		qa.qcri.aidr.task.entities.Document doc2 = new qa.qcri.aidr.task.entities.Document(12345679L, true);
		String jsonString = mapper.serializeTask(doc);
		Document newDoc1 = mapper.transformDocument(doc);
		System.out.println("New document 1 = " + newDoc1.getDocumentID());

		Document newDoc2 = mapper.deSerialize(jsonString, Document.class);
		System.out.println("New document 2 = " + newDoc2.getDocumentID());

		List<qa.qcri.aidr.task.entities.Document> docList = new ArrayList<qa.qcri.aidr.task.entities.Document>();
		docList.add(doc);
		docList.add(doc2);
		String jsonString2 = mapper.serializeTask(docList);

		List<Document> newDocList = mapper.deSerializeList(jsonString2, new TypeReference<List<Document>>() {});
		for (Document d: newDocList) {
			System.out.println("New document = " + d.getDocumentID());
		}

	}
}
