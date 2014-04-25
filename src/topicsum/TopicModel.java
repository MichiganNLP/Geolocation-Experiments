package topicsum;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class TopicModel {

	private String className;
	private HashMap<String, Double> distribution;

	public TopicModel(String className) {
		this.className = className;
		this.distribution = new HashMap<String, Double>();
	}

	public void addValue(String word, Double value) {
		distribution.put(word, value);
	}

	public static TopicModel fromFile(File file) {
		String className = file.getName();
		TopicModel model = new TopicModel(className);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			reader.lines().forEach((line) -> model.parseLine(line));
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return model;
	}

	public void parseLine(String line) {
		String[] sections = line.split("\t");
		addValue(sections[0], Double.parseDouble(sections[1]));
	}

	public Double getWordProb(String word) {
		return distribution.getOrDefault(word, 0.0);
	}

	public Double getSentenceProb(String[] words) {
		Double prob = 0.0;
		for (String word : words) {
			prob += getWordProb(word);
		}
		return prob;
	}

	@Override
	public String toString() {
		return className;
	}
}
