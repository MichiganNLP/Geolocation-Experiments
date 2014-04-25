package mypersonality;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import max.nlp.wrappers.mypersonality.objects.Location;
import max.nlp.wrappers.mypersonality.objects.Status;
import max.nlp.wrappers.mypersonality.objects.User;
import max.nlp.wrappers.mypersonality.parsers.Reader;

public class PrintUsersByLocation {

	public static String root = "/home/jmaxk/temp/fb/data/hometownLoc/";

	private Map<String, PrintWriter> writers = new HashMap<String, PrintWriter>();
	private Map<String, User> indexedUsers;
	private Reader r;

	public PrintUsersByLocation() {
		r = new Reader();
		r.loadLocDict();
		r.loadUsersAndIndex(User.hasHometownLoc);
	}



	public void test() {
		Entry<String, User> user1 = indexedUsers.entrySet().iterator().next();
		System.out.println(user1.getKey());

		User user = indexedUsers.get(user1.getKey());
		System.out.println(user.getHomeLocation().getState());
	}

	public static void main(String[] args) {
		PrintUsersByLocation p = new PrintUsersByLocation();
		// p.printHometownLocs(root);
		p.test();

	}

	public Consumer<String> parseAndPrint = (String line) -> {
		String[] sects = line.split(",");
		String id = sects[0].replaceAll("\"", "");
		User u = indexedUsers.get(id);
		if (u != null) {
			PrintWriter pw = findOutputFileForLocation(u.getHomeLocation(), root);
			String userId = sects[0];
			String date = sects[1];
			String content = sects[2];
			Status status = new Status(userId, content, date);
			pw.println(status.getCleanStatus());

		}
	};

	public PrintWriter findOutputFileForLocation(Location l, String outputDir) {
		String state = l.getState();
		PrintWriter pw = writers.get(state);
		if (pw == null) {
			File stateFile = new File(root + state);
			try {
				pw = new PrintWriter(new FileWriter(stateFile));
				writers.put(state, pw);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return pw;
	}
}
