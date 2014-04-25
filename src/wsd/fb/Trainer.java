package wsd.fb;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import max.nlp.wsd.w2vec.Learn;
import max.nlp.wsd.w2vec.Word2VEC;

public class Trainer {
	
	public Trainer(String trainDir){
		this.trainDir = trainDir;
	}
	/**
	 * This must contain the facebook data split by state, and each state file must end in .txt
	 */
	private  String trainDir = "/home/jmaxk/temp/fb/data/";

	public void trainOnFbData(String saveDir) {
		File[] files = new File(trainDir).listFiles();
		for (File dataFile : files) {
			String fileName = dataFile.getName();
			if (fileName.endsWith(".txt")) {
				String nameWithoutExtension = fileName.replaceAll("\\.txt", "");
				File modelFile = new File(saveDir + nameWithoutExtension
						+ ".bin");
				Learn lean = new Learn();
				try {
					lean.learnFile(dataFile);
					lean.saveModel(modelFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static Map<String, Word2VEC> loadFBData(boolean google, String dataDir) {
		Map<String, Word2VEC> vecs = new HashMap<String, Word2VEC>();
		File[] files = new File(dataDir).listFiles();
		for (File modelFile : files) {
			String modelFileName = modelFile.getName();
			Word2VEC w2v = new Word2VEC();
			if (modelFileName.endsWith(".bin")) {
				String nameWithoutExtension = modelFileName.replaceAll(
						"\\.bin", "").replaceAll("\\.txt", "");
				try {
					if (google)
						w2v.loadGoogleModel(modelFile.getAbsolutePath());
					else
						w2v.loadJavaModel(modelFile.getAbsolutePath());
					vecs.put(nameWithoutExtension, w2v);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return vecs;
	}
}
