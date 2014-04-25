package topicsum;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

public class Experiment {

//	public static String dir = "/home/jmaxk/temp/tm/models/shrunk500/";
//	String data = "Only in South Carolina can you drive by two Nascar drivers pulled over by state patrol.  Who wants to wager it was for speeding.";

	public List<TopicModel> topicModels = new ArrayList<TopicModel>();

	public static void main(String[] args) {
		String tm_dir = "/home/jmaxk/temp/tm/models/shrunk500/";
		String data = "Only in South Carolina can you drive by two Nascar drivers pulled over by state patrol.";
		Experiment e = new Experiment();
		e.loadTopicModels(tm_dir);
		System.out.println("loaded");
		Map<TopicModel, Double> probs = e.findProbabilities(data);
		SortedSet<Entry<TopicModel, Double>> results = MapSorter
				.entriesSortedByValues(probs, false);
		Iterator<Entry<TopicModel, Double>> itr = results.iterator();
		while (itr.hasNext())
			System.out.println(itr.next());
	}

	public void loadTopicModels(String root) {
		for (File model : new File(root).listFiles()) {
			topicModels.add(TopicModel.fromFile(model));
		}
	}

	public Map<TopicModel, Double> findProbabilities(String data) {
		String[] words = data.split(" ");
		Map<TopicModel, Double> modelProbs = new ConcurrentHashMap<TopicModel, Double>();
		topicModels.parallelStream().forEach(
				model -> modelProbs.put(model, model.getSentenceProb(words)));
		return modelProbs;
	}
}
