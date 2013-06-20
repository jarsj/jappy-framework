import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;


public class Break {
	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(args[0]))));
		
		String s = null;
		while ((s = br.readLine()) != null) {
			String sp[] = s.split("&");
			for (int i = 0; i < sp.length; i++) {
				if (sp[i].startsWith("q=")) {
					System.out.println(sp[i].substring(2));
					continue;
				}
			}
		}
	}
}
