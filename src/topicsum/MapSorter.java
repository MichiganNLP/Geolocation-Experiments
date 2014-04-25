package topicsum;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class MapSorter {

	@SuppressWarnings("rawtypes")
	public static <K, V extends Comparable> SortedSet<Map.Entry<K, V>> entriesSortedByValues(
			Map<K, V> map, final boolean reverse) {
		SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
				new Comparator<Map.Entry<K, V>>() {
					@SuppressWarnings("unchecked")
					public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
						if (reverse) {
							int res = e1.getValue().compareTo(e2.getValue());
							return res != 0 ? res : 1;
						} else {
							int res = -e1.getValue().compareTo(e2.getValue());
							return res != 0 ? res : 1;

						}
					}
				});
		sortedEntries.addAll(map.entrySet());
		return sortedEntries;
	}


}