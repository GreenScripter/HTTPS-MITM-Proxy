import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class Readers {
	public static Object writeLock = new Object();
	
	/**
	 * Writes the urls passed through to a file.
	 */
	public static ContentReader urlRecorder() {
		return new ContentReader() {
			
			public void readOutgoingHTTP(Request r) {
				System.out.println(r.getHeader("host") + r.path);
				File saver = new File("recordedURLs");
				synchronized (writeLock) {
					try {
						OutputStream out = new FileOutputStream(saver, true);
						out.write((r.getHeader("host") + r.path + "\n").getBytes());
						out.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			public void readIncomingHTTP(Response r) {}
			
		};
	}
	
	/**
	 * Records the body of http requests to files.
	 */
	public static ContentReader contentRecorder() {
		return new ContentReader() {
			
			public void readOutgoingHTTP(Request r) {
				System.out.println(r.getHeader("host") + r.path);
				File saver = new File("contentRecordings");
				saver.mkdir();
				synchronized (writeLock) {
					try {
						if (r.body != null) {
							OutputStream out = new FileOutputStream(new File(saver, r.path.replace("/", "-")));
							out.write(r.body);
							out.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			public void readIncomingHTTP(Response r) {}
			
		};
	}
}
