package wsd.fb;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import max.nlp.util.basic.MapSorter;
import max.nlp.wrappers.mypersonality.objects.Status;
import max.nlp.wrappers.mypersonality.objects.User;
import max.nlp.wrappers.mypersonality.parsers.Reader;
import max.nlp.wrappers.stanford.StanfordNLP;
import max.nlp.wsd.w2vec.Word2VEC;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

public class Experiment {

	// config variables
	
	//directory with w2vec trained vectors
	public final static String dataDir = "/home/jmaxk/temp/fb/data/wordVecs";
	
	//output files
	public final static String output = "/home/jmaxk/temp/fb/results.txt";

	// public final static String dataDir =
	// "/home/jmaxk/wsdExperiments/facebook/current";
	// public final static String output =
	// "/home/jmaxk/wsdExperiments/results.txt";
	public final static Integer N = 10;

	// Class variables
	private StanfordNLP nlp;
	private Map<String, Word2VEC> vectors;
	private Map<String, List<User>> usersIndexedByState;
	private Map<String, List<Status>> statusesIndexedByUser;
	private PrintWriter outputWriter;

	public static void main(String[] args) {
		Experiment e = new Experiment();
		e.evaluate();

	}

	public void evaluate() {
		usersIndexedByState.entrySet().parallelStream()
				.forEach(evaluateAccuracyOnState);
		outputWriter.flush();
		outputWriter.close();
	}

	public List<String> mergeList(List<String> a, List<String> b) {
		a.addAll(b);
		return a;
	}

	public Map<String, Context> findStateVectorsForWord(String word) {
		Map<String, Context> stateContexts = new HashMap<String, Context>();
		for (Entry<String, Word2VEC> e : vectors.entrySet()) {
			String key = e.getKey();
			List<String> contextVectors = e.getValue().distance(word).stream()
					.map(w -> w.name).collect(Collectors.toList());
			stateContexts.put(key, new Context(contextVectors));
		}
		return stateContexts;
	}

	public Map<String, Double> updateStateScores(Map<String, Double> scores,
			String state, double toAdd) {
		Double oldScore = scores.getOrDefault(state, 0.0);
		scores.put(state, oldScore + toAdd);
		return scores;
	}

	public Map<String, Context> statusToContexts(Status s) {
		Map<String, Context> contexts = new HashMap<String, Context>();
		String cleanStatus = s.getCleanStatus();
		if (cleanStatus.length() <= 5 || cleanStatus.isEmpty())
			return contexts;
		List<CoreMap> annotated = nlp.annotate(cleanStatus);
		if (annotated.isEmpty())
			return contexts;
		CoreMap annotatedText = annotated.get(0);
		List<String> nouns = new ArrayList<String>();
		for (CoreLabel token : annotatedText.get(TokensAnnotation.class)) {
			String pos = token.get(PartOfSpeechAnnotation.class);
			if (pos.equals("NN")) {
				String word = token.get(TextAnnotation.class);
				nouns.add(word);
			}
		}

		if (nouns.isEmpty())
			return contexts;
		int endIndex = nouns.size();
		contexts.put(nouns.get(0), new Context(nouns.subList(1, endIndex)));
		if (contexts.size() > 1) {
			for (int i = 1; i < endIndex; i++) {
				List<String> before = new ArrayList<String>(nouns.subList(0, i));
				String key = nouns.get(i);
				List<String> after = new ArrayList<String>(nouns.subList(i + 1,
						endIndex));
				after.addAll(before);
				contexts.put(key, new Context(after));
			}
		}
		return contexts;
	}

	public int computeContextOverlap(Context a, Context b) {
		int overlap = 0;
		List<String> aContext = a.getContext();
		List<String> bContext = b.getContext();
		if (aContext.isEmpty() || bContext.isEmpty())
			return 0;
		for (String word : aContext) {
			if (bContext.contains(word))
				overlap++;
		}
		return overlap;
	}

	public Consumer<Entry<String, List<User>>> evaluateAccuracyOnState = (
			usersForState) -> {

		String desiredState = usersForState.getKey();
		List<User> users = usersForState.getValue();
		outputWriter.println("Evaluating " + desiredState + " which has "
				+ users.size() + " users");
		Map<String, Map<String, Context>> definitions = new HashMap<String, Map<String, Context>>();
		int correctlyClassifiedUsers = 0;
		int totalUsers = 0;

		for (User u : users) {
			List<Status> statuses = statusesIndexedByUser.getOrDefault(
					u.getId(), new ArrayList<Status>());
			Map<String, Double> stateScores = new HashMap<String, Double>();
			if (!statuses.isEmpty()) {
				totalUsers++;
				for (Status s : statuses) {
					Map<String, Context> statusNounsWithContext = statusToContexts(s);

					// wsd every word in the status
					for (Entry<String, Context> statusNounWithContext : statusNounsWithContext
							.entrySet()) {
						String wordToBeDisamiguated = statusNounWithContext
								.getKey();
						Context contextForStatus = statusNounWithContext
								.getValue();

						Map<String, Context> contextWordsInState = definitions
								.get(wordToBeDisamiguated);
						if (contextWordsInState == null) {
							contextWordsInState = findStateVectorsForWord(wordToBeDisamiguated);
							definitions.put(wordToBeDisamiguated,
									contextWordsInState);
						}
						// score it for each state
						for (Entry<String, Context> stateContext : contextWordsInState
								.entrySet()) {
							String state = stateContext.getKey();
							Context contextForState = stateContext.getValue();
							int stateScore = computeContextOverlap(
									contextForStatus, contextForState);
							updateStateScores(stateScores, state, stateScore);
						}
					}
				}
			}

			// Classify
			MapSorter<String, Double> sorter = new MapSorter<String, Double>();
			List<Entry<String, Double>> sortedResults = sorter
					.sortMap(stateScores);
			boolean correct = isCorrect(sortedResults, N, desiredState);
			if (correct) {
				correctlyClassifiedUsers++;
			}

		}

		outputWriter.println(correctlyClassifiedUsers
				+ " users correctly classified out of " + totalUsers);
		outputWriter.flush();
	};

	public Experiment() {
		// StanfordNLP for tokenizing/extracting POS
		nlp = StanfordNLP.getInstance();
		// w2vec data for computing distance
		vectors = Trainer.loadFBData(true, dataDir);

		// Read in my personality data, and index users/statuses for fast access
		Reader r = new Reader();
		r.loadLocDict();
		r.loadUsersAndIndex((u) -> u.getHomeLocation() != null
				&& u.getHomeLocation().getState() != null);

		r.indexStatusesByUsers();
		usersIndexedByState = r.getUsersIndexedByState();
		statusesIndexedByUser = r.getStatusIndexedByUser();

		// for writing the output
		try {
			outputWriter = new PrintWriter(new FileWriter(output));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	Function<String, CoreMap> annotateSentence = (String txt) -> {
		List<CoreMap> sentences = nlp.annotate(txt);
		return sentences.get(0);
	};

	Function<CoreMap, List<String>> extractNouns = sentence -> {
		List<String> nouns = new ArrayList<String>();
		for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
			String pos = token.get(PartOfSpeechAnnotation.class);
			if (pos.equals("NN")) {
				String word = token.get(TextAnnotation.class);
				nouns.add(word);
			}
		}
		return nouns;
	};

	public boolean isCorrect(List<Entry<String, Double>> sortedResults, int N,
			String correctState) {

		int i = 0;
		boolean correct = false;
		for (Entry<String, Double> stateWithScore : sortedResults) {
			if (++i > N)
				break;
			if (stateWithScore.getKey().equals(correctState))
				correct = true;
		}
		return correct;
	}

	//
	public List<CoreMap> stringToAnnotatedSentence(String txt) {
		List<CoreMap> sentences = nlp.annotate(txt);
		return sentences;

	}

}
