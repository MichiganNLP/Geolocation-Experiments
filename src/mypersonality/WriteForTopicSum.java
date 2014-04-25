package mypersonality;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import max.nlp.wrappers.mypersonality.objects.Location;
import max.nlp.wrappers.mypersonality.objects.Status;
import max.nlp.wrappers.mypersonality.objects.User;
import max.nlp.wrappers.mypersonality.parsers.Reader;

public class WriteForTopicSum {

	//where to write
	private static String OUTPUT_DIR = "/home/jmaxk/resources/mypersonality/output/full-current/";

	/**
	 * Shrinks an already-created dataset
	 * @param maxLines, max # of lines to keep per file 
	 * @param oldDir, the old directory
	 * @param newDir, the new shrunk directory
	 */
	public static void shrink(int maxLines, String oldDir, String newDir) {
		for (File languageDir : new File(oldDir).listFiles()) {
			File root = new File(newDir + languageDir.getName());
			root.mkdir();
			for (File languageFile : languageDir.listFiles()) {
				try {
					File newFile = new File(root + "/" + languageFile.getName());
					System.out.println(newFile);
					BufferedReader b = new BufferedReader(new FileReader(
							languageFile));
					PrintWriter w = new PrintWriter(new FileWriter(newFile));
					String line = "";
					int counter = 0;
					while ((line = b.readLine()) != null) {
						if (counter++ >= maxLines)
							break;
						w.println(line);
					}
					w.flush();
					w.close();
					b.close();

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//
			}
		}
		
	}

	public static void main(String[] args) {
		Reader r = new Reader();
		r.loadUsersAndIndex(User.hasHometownLoc);
		r.indexStatusesByUsers();

		writeData(r, OUTPUT_DIR);
	}

	public void finish() {
		for (PrintWriter w : writers.values()) {
			w.flush();
			w.close();
		}
	}

	/**
	 * Writes the data for topic weave. Takes in the data from mypersonality, and segments it so that each user
	 * belongs to one of the 'super city' regions from 
	 * Bo Han, Paul Cook and Timothy Baldwin, 
	 * A Stacking-based Approach to Twitter User Geolocation Prediction, 
	 * In ACL 2013, Demo Session, pages 7–12, Sofia, Bulgaria
	 * 
	 * The output format is as follows
	 * 
	 * root/
	 * 	State1
	 * 	  region1/
	 * 	  region2
	 *  State2/
	 *    region3/
	 *    ...
	 */
	public static void writeData(Reader r, String outputDir) {
		Map<String, List<Status>> indexedStatuses = r.getStatusIndexedByUser();
		Map<String, User> indexedUsers = r.getUsersIndexedByID();
		for (Entry<String, List<Status>> userWithStatuses : indexedStatuses.entrySet()){
			
			//get a reference to the users for these statuses
			User u = indexedUsers.get(userWithStatuses.getKey());
			
			//find the closest location to that user. see CoordSearch for what closest location means 
			Location closest = CoordSearch.findClosestLocatoin(u.getCurrentLoc()
					.getLatitutde(), u.getCurrentLoc().getLongitude());
			
			//write their statuses to the file for their states
			PrintWriter w = findPrintWriter(closest, outputDir);
			for (Status s : userWithStatuses.getValue()) {
				w.println(s.getStatus());
			}
		}


	}

	private static Map<File, PrintWriter> writers = new HashMap<File, PrintWriter>();

	public static PrintWriter findPrintWriter(Location l, String locOutput) {
		File dir = new File(locOutput + "/" + l.getState());
		if (!dir.exists())
			dir.mkdir();
		File langFile = new File(dir + "/" + l.getCity());
		PrintWriter w = writers.get(langFile);
		if (w == null) {
			try {
				w = new PrintWriter(new FileWriter(langFile, true));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			writers.put(langFile, w);
		}
		return w;
	}
}
