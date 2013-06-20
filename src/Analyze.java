import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.ibm.icu.text.Collator.ReorderCodes;


public class Analyze {
	
	public static void main(String[] args) throws Exception {
		String pattern1 = args[1];
		String pattern2 = args[2];
		int bad = Integer.parseInt(args[3]);
		int count = Integer.parseInt(args[4]);
		
		ArrayList<String> records = new ArrayList<String>();
		HashMap<String, LinkedList<Integer>> last_logs = new HashMap<String, LinkedList<Integer>>();
		HashMap<String, LinkedList<Integer>> last = new HashMap<String, LinkedList<Integer>>();
		TreeSet<Integer> badRecords = new TreeSet<Integer>();
		TreeSet<Integer> allUsers = new TreeSet<Integer>();
		TreeSet<Integer> badUsers = new TreeSet<Integer>();
		
		int badNumber = 0;
		int total = 0;
		
		int rid = -1;
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(args[0]))));
		
		String s = null;
		while ((s = br.readLine()) != null) {
			records.add(s);
			rid++;
			if (!s.contains(pattern1))
				continue;
			String sp[] = s.split(" ");
			if (!sp[3].contains(pattern2))
				continue;
			String uid = sp[1];
			
			if (uid == null || uid.length() == 0 || uid.equals("0"))
				continue;
			
			try {
				allUsers.add(Integer.parseInt(uid));
			} catch (Exception e) {
				continue;
			}
			
			total++;
			if (total % 1000 == 0) {
//				System.out.println("SAMPLE=" + s);
			}
			
			int hour = Integer.parseInt(sp[0].split(":")[0]);
			int min = Integer.parseInt(sp[0].split(":")[1]);

			int ts = hour * 60 + min;
			
			if (last.containsKey(uid) && (last.get(uid).size() == count)) {
				LinkedList<Integer> seq = last.get(uid);
				boolean countIt = true;
				for (int i = 0; i < seq.size() - 1; i++) {
					if (seq.get(i + 1) - seq.get(i) > bad) {
						countIt = false;
					}
				}
				if (countIt) {
					badUsers.add(Integer.parseInt(uid));
					badNumber++;
					for (int ss : last_logs.get(uid)) {
						badRecords.add(ss);
					}
				}
			}
			
			if (!last.containsKey(uid)) {
				last.put(uid, new LinkedList<Integer>());
				last_logs.put(uid, new LinkedList<Integer>());
			}
			LinkedList<Integer> seq = last.get(uid);
			LinkedList<Integer> seq_logs = last_logs.get(uid);
					
			seq.addLast(ts);
			seq_logs.addLast(rid);
			if (seq.size() > count) {
				seq.removeFirst();
				seq_logs.removeFirst();
			}
		}
		
		System.out.println("TotalRecords=" + total);
		System.out.println("BadRecords=" + badRecords.size());
		System.out.println("Pecentage=" + (badRecords.size() * 100.0) / total);
		System.out.println();
		System.out.println("TotalUsers=" + allUsers.size());
		System.out.println("BadUsers=" + badUsers.size());
		System.out.println("Pecentage=" + (badUsers.size() * 100.0) / allUsers.size());
		
		for (int b : badRecords) {
			System.out.println(records.get(b));
		}
	}
}
