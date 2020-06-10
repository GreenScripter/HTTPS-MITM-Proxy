import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

// Newer versions of this class exist, designed to support low level modification of http with
// minimal restrictions, but doesn't support compression very well.
public class UploadTest {
	
	/**
	 * Reads exactly one unicode character from a stream.
	 * 
	 * @param in input stream to read from
	 * @return that character
	 */
	public static String readChar(InputStream in) throws IOException {
		int theByte = in.read();
		if (theByte == -1) {
			throw new EOFException();
			
		}
		boolean a = (theByte & 128) != 0;
		boolean b = (theByte & 64) != 0;
		boolean c = (theByte & 32) != 0;
		boolean d = (theByte & 16) != 0;
		int ai = a ? 1 : 0;
		int bi = b ? 1 : 0;
		int ci = c ? 1 : 0;
		int di = d ? 1 : 0;
		if (ai == 1 & bi == 1) {
			
			if (ci == 1) {
				if (di == 1) {
					
					int b2 = in.read();
					int b3 = in.read();
					int b4 = in.read();
					if (b2 == -1 || b3 == -1 || b4 == -1) {
						throw new EOFException();
					}
					
					return new String(new byte[] { (byte) theByte, (byte) b2, (byte) b3, (byte) b4 }, StandardCharsets.UTF_8);
				} else {
					int b2 = in.read();
					int b3 = in.read();
					if (b2 == -1 || b3 == -1) {
						throw new EOFException();
					}
					return new String(new byte[] { (byte) theByte, (byte) b2, (byte) b3 }, StandardCharsets.UTF_8);
				}
			} else {
				int b2 = in.read();
				if (b2 == -1) {
					throw new EOFException();
				}
				return new String(new byte[] { (byte) theByte, (byte) b2 }, StandardCharsets.UTF_8);
			}
			
		} else {
			return new String(new byte[] { (byte) theByte }, StandardCharsets.UTF_8);
		}
	}
	
	/**
	 * Read exactly one line from an inputstream, no buffering.
	 * 
	 * @param is stream to read from
	 * @return line read
	 */
	public static String readLine(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		String c = readChar(is);
		if (c.equals("\r")) {
			c = readChar(is);
		}
		try {
			while (!c.equals("\n")) {
				sb.append(c);
				c = readChar(is);
				if (c.equals("\r")) {
					c = readChar(is);
				}
			}
		} catch (EOFException e) {
			
		}
		return sb.toString();
	}
	
	/**
	 * Read an HTTP response from an input stream.
	 */
	public static Response readResponse(InputStream in) throws IOException {
		String inputLine;
		List<String> inHeaders = new ArrayList<>();
		List<String> inValues = new ArrayList<>();
		int responseCode = 0;
		String responseText = "";
		int c = 0;
		while ((inputLine = readLine(in)) != null) {
			if (c == 0) {
				responseCode = Integer.parseInt(inputLine.substring(inputLine.indexOf(" ") + 1, inputLine.indexOf(" ") + 4));
				try {
					responseText = inputLine.substring(inputLine.indexOf(" ") + 5);
				} catch (Exception e) {
					
				}
			} else {
				if (inputLine.length() == 0) {
					break;
				}
				String headerLine = inputLine.substring(0, inputLine.indexOf(": "));
				String valueLine = inputLine.substring(inputLine.indexOf(": ") + 2);
				inHeaders.add(headerLine);
				inValues.add(valueLine);
			}
			c++;
		}
		int length = -1;
		for (int i = 0; i < inHeaders.size(); i++) {
			if (inHeaders.get(i).equalsIgnoreCase("Content-Length")) {
				length = Integer.parseInt(inValues.get(i));
				break;
			}
		}
		byte[] body = new byte[0];
		
		Response r = new Response();
		
		r.body = body;
		r.inHeaders = inHeaders;
		r.inValues = inValues;
		r.responseCode = responseCode;
		r.responseText = responseText;
		r.compression = false;
		if (r.getHeader("Content-Encoding") != null && r.getHeader("Content-Encoding").equals("gzip")) {
			r.compression = true;
		}
		if (r.getHeader("Transfer-Encoding") != null && r.getHeader("Transfer-Encoding").equals("chunked")) {
			int chunklength = Integer.parseInt(readLine(in), 16);
			
			System.out.println("Length " + chunklength);
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			DataInputStream ind = new DataInputStream(in);
			
			while (chunklength != 0) {
				body = new byte[chunklength];
				ind.readFully(body);
				
				out.write(body);
				
				System.out.println("Content " + new String(body));
				
				readLine(in);
				
				chunklength = Integer.parseInt(readLine(in), 16);
				System.out.println("Length " + chunklength);
				
			}
			readLine(in);
			
			r.body = out.toByteArray();
			for (int i = 0; i < inHeaders.size(); i++) {
				if (inHeaders.get(i).equalsIgnoreCase("Transfer-Encoding")) {
					inHeaders.remove(i);
					inValues.remove(i);
					break;
				}
			}
			inHeaders.add("Content-Length");
			inValues.add(r.body.length + "");
		} else if (length == -1) {
		} else {
			body = new byte[length];
			r.body = body;
			
			DataInputStream ind = new DataInputStream(in);
			ind.readFully(body);
			
		}
		r.body = r.compression ? gunzip(r.body) : r.body;
		
		return r;
	}
	
	/**
	 * Read an HTTP request from an input stream.
	 */
	public static Request readRequest(InputStream in) throws IOException {
		String inputLine;
		List<String> inHeaders = new ArrayList<>();
		List<String> inValues = new ArrayList<>();
		String method = "";
		String path = "";
		String version = "";
		int c = 0;
		while ((inputLine = readLine(in)) != null) {
			if (c == 0) {
				String[] parts = inputLine.split(" ");
				method = parts[0];
				path = parts[1];
				version = parts[2];
			} else {
				if (inputLine.length() == 0) {
					break;
				}
				String headerLine = inputLine.substring(0, inputLine.indexOf(": "));
				String valueLine = inputLine.substring(inputLine.indexOf(": ") + 2);
				inHeaders.add(headerLine);
				inValues.add(valueLine);
			}
			c++;
		}
		int length = 0;
		for (int i = 0; i < inHeaders.size(); i++) {
			if (inHeaders.get(i).equalsIgnoreCase("Content-Length")) {
				length = Integer.parseInt(inValues.get(i));
				break;
			}
		}
		byte[] body = new byte[length];
		
		Request r = new Request();
		
		r.body = body;
		r.inHeaders = inHeaders;
		r.inValues = inValues;
		r.method = method;
		r.path = path;
		r.version = version;
		
		DataInputStream ind = new DataInputStream(in);
		ind.readFully(body);
		return r;
	}
	
	/**
	 * Gzip bytes.
	 */
	public static byte[] gzip(byte[] input) {
		GZIPOutputStream gzipOS = null;
		try {
			ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
			gzipOS = new GZIPOutputStream(byteArrayOS);
			gzipOS.write(input);
			gzipOS.flush();
			gzipOS.close();
			return byteArrayOS.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (gzipOS != null) {
				try {
					gzipOS.close();
				} catch (Exception ignored) {
				}
			}
		}
	}
	
	/**
	 * Un-gzip bytes.
	 */
	public static byte[] gunzip(byte[] input) {
		GZIPInputStream gzipOS = null;
		try {
			ByteArrayInputStream byteArrayOS = new ByteArrayInputStream(input);
			gzipOS = new GZIPInputStream(byteArrayOS);
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] b = new byte[8192];
			int read = 0;
			while ((read = gzipOS.read(b)) != -1) {
				out.write(b, 0, read);
			}
			gzipOS.close();
			return out.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (gzipOS != null) {
				try {
					gzipOS.close();
				} catch (Exception ignored) {
				}
			}
		}
	}
}

class Response {
	
	byte[] body;
	List<String> inHeaders = new ArrayList<>();
	List<String> inValues = new ArrayList<>();
	int responseCode;
	String responseText;
	boolean compression;
	
	/**
	 * Convert the response to bytes.
	 */
	public byte[] getBytes() {
		byte[] body = this.body;
		if (body != null) {
			body = (compression ? UploadTest.gzip(body) : body);
			
		}
		for (int i = 0; i < inHeaders.size(); i++) {
			if (inHeaders.get(i).equalsIgnoreCase("Content-Length")) {
				inValues.set(i, body.length + "");
				break;
			}
		}
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StringBuffer header = new StringBuffer();
		
		header.append("HTTP/1.1");
		header.append(" ");
		header.append(responseCode + "");
		header.append(" ");
		header.append(responseText);
		
		header.append("\r\n");
		
		for (int i = 0; i < inHeaders.size(); i++) {
			header.append(inHeaders.get(i));
			header.append(": ");
			header.append(inValues.get(i));
			header.append("\r\n");
			
		}
		header.append("\r\n");
		try {
			out.write(header.toString().getBytes());
			if (body != null) out.write(compression ? UploadTest.gzip(body) : body);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return out.toByteArray();
		
	}
	
	/**
	 * Get the first instance of a header by name.
	 * 
	 * @param name header name
	 * @return first header instance or null
	 */
	public String getHeader(String name) {
		for (int i = 0; i < inHeaders.size(); i++) {
			if (inHeaders.get(i).equalsIgnoreCase(name)) {
				return inValues.get(i);
			}
		}
		return null;
	}
	
	/**
	 * Remove any Accept-Encoding headers that this client may not understand.
	 */
	public void disableCompression() {
		for (int i = 0; i < inHeaders.size(); i++) {
			if (inHeaders.get(i).equalsIgnoreCase("Content-Encoding")) {
				inHeaders.remove(i);
				inValues.remove(i);
				compression = false;
				break;
			}
		}
	}
	
	/**
	 * Replace any Accept-Encoding headers with gzip, which is understood by this encoder
	 */
	public void gzipOnlyCompression() {
		for (int i = 0; i < inHeaders.size(); i++) {
			if (inHeaders.get(i).equalsIgnoreCase("Content-Encoding")) {
				inValues.set(i, "gzip");
				compression = true;
				break;
			}
		}
	}
	
}

class Request {
	
	byte[] body;
	List<String> inHeaders = new ArrayList<>();
	List<String> inValues = new ArrayList<>();
	
	String method;
	String version;
	String path;
	
	/**
	 * Convert the Request to bytes.
	 */
	public byte[] getBytes() {
		for (int i = 0; i < inHeaders.size(); i++) {
			if (inHeaders.get(i).equalsIgnoreCase("Content-Length")) {
				inValues.set(i, body.length + "");
				break;
			}
		}
		
		if (body.length == 0) {
			for (int i = 0; i < inHeaders.size(); i++) {
				if (inHeaders.get(i).equalsIgnoreCase("Content-Length")) {
					inValues.remove(i);
					inHeaders.remove(i);
					break;
				}
			}
		}
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StringBuffer header = new StringBuffer();
		
		header.append(method);
		header.append(" ");
		header.append(path);
		header.append(" ");
		header.append(version);
		
		header.append("\r\n");
		
		for (int i = 0; i < inHeaders.size(); i++) {
			header.append(inHeaders.get(i));
			header.append(": ");
			header.append(inValues.get(i));
			header.append("\r\n");
			
		}
		header.append("\r\n");
		try {
			out.write(header.toString().getBytes());
			out.write(body);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return out.toByteArray();
		
	}
	
	/**
	 * Get the first instance of a header by name.
	 * 
	 * @param name header name
	 * @return first header instance or null
	 */
	public String getHeader(String name) {
		for (int i = 0; i < inHeaders.size(); i++) {
			if (inHeaders.get(i).equalsIgnoreCase(name)) {
				return inValues.get(i);
			}
		}
		return null;
	}
	
	/**
	 * Remove any Accept-Encoding headers that this client may not understand.
	 */
	public void disableCompression() {
		for (int i = 0; i < inHeaders.size(); i++) {
			if (inHeaders.get(i).equalsIgnoreCase("Accept-Encoding")) {
				inHeaders.remove(i);
				inValues.remove(i);
				
				break;
			}
		}
	}
	
	/**
	 * Replace any Accept-Encoding headers with gzip, which is understood by this encoder
	 */
	public void gzipOnlyCompression() {
		for (int i = 0; i < inHeaders.size(); i++) {
			if (inHeaders.get(i).equalsIgnoreCase("Accept-Encoding")) {
				inValues.set(i, "gzip");
				break;
			}
		}
	}
}