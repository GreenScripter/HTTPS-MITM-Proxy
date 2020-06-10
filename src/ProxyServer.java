import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ProxyServer {
	
	static String[] toblock;
	static String[] blockUsers;
	
	public static void main(String[] args) throws Exception {
		//				if (!certs.containsKey("192.168.1.5")) {
		//					certs.put("192.168.1.5", CertificateGenerator.generateCertificate("192.168.1.5", null));
		//				}
		//				KeyStore ks = certs.get("192.168.1.5");
		//				ks.store(new FileOutputStream(new File("./gserver.keystore")), "password".toCharArray());
		toblock = (new String(fileToBytes(new File("toblock.txt"))) + "\n" + new String(fileToBytes(new File("toblock2.txt")))).split("\\n");
		List<String> block = new ArrayList<>();
		for (String s : toblock) {
			if (!s.isEmpty()) {
				block.add(s);
			} else {
				System.out.println("Removed blank blockage");
			}
		}
		toblock = block.toArray(new String[0]);
		if (new File("BlockUsers.txt").exists()) {
			new File("BlockUsers.txt");
		}
		blockUsers = (new String(fileToBytes(new File("BlockUsers.txt")))).split("\\n");
		
		new ProxyServer(7777);
	}
	
	public static byte[] fileToBytes(File file) throws IOException {
		if (!file.exists()) {
			return new byte[0];
		}
		byte[] fileContent = Files.readAllBytes(file.toPath());
		return fileContent;
	}
	
	public ProxyServer(int port) throws IOException {
		ServerSocket ss = new ServerSocket(port);
		
		new Thread(() -> {
			try {
				while (true) {
					Socket s = ss.accept();
					connect(s);
				}
			} catch (IOException e) {
				try {
					ss.close();
				} catch (IOException e1) {
				}
				e.printStackTrace();
			}
		}).start();
	}
	
	/**
	 * Handle an incoming proxy connection.
	 */
	public void connect(Socket client) {
		new Thread(() -> {
			String user = client.getInetAddress().getHostAddress();
			//Block any ips listed in the file.
			for (String s : blockUsers) {
				if (s.equals(user)) {
					System.out.println("Blocked " + user);
					try {
						Response reply = new Response();
						reply.responseCode = 403;
						reply.responseText = "Forbidden";
						client.getOutputStream().write(reply.getBytes());
						client.close();
						return;
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
				}
			}
			try {
				//Read the request to the proxy.
				Request r = UploadTest.readRequest(client.getInputStream());
				String toConnect = r.path;
				if (toConnect.startsWith("http://")) {
					toConnect = toConnect.substring(toConnect.indexOf("/") + 2);
					
				}
				if (toConnect.contains("/")) {
					toConnect = toConnect.substring(0, toConnect.indexOf("/"));
				}
				Socket s;
				String domain = "";
				
				//Check if port is specified.
				if (toConnect.contains(":")) {
					domain = toConnect.substring(0, toConnect.indexOf(":"));
					System.out.println(client.getInetAddress().getHostAddress() + ": " + toConnect);
					
					//Block any known add domains.
					for (String ad : ProxyServer.toblock) {
						if (domain.contains(ad)) {
							Response reply = new Response();
							reply.responseCode = 502;
							reply.responseText = "Bad Gateway";
							client.getOutputStream().write(reply.getBytes());
							throw new IOException();
							
						}
					}
					
					s = new Socket(toConnect.substring(0, toConnect.indexOf(":")), Integer.parseInt(toConnect.substring(toConnect.indexOf(":") + 1)));
				} else {
					domain = toConnect;
					//Block any known add domains.
					for (String ad : ProxyServer.toblock) {
						if (domain.contains(ad)) {
							client.close();
							throw new IOException();
						}
					}
					System.out.println(toConnect);
					
					s = new Socket(toConnect, 80);
				}
				//If request was https, it will be a connect request, and we need to tell the client we connected them.
				if (r.method.equals("CONNECT")) {
					Response reply = new Response();
					reply.responseCode = 200;
					reply.responseText = "Connection Established";
					client.getOutputStream().write(reply.getBytes());
				}
				
				SocketConnector sc = new SocketConnector(client, s);
				if (r.method.equals("CONNECT")) {
					//ssl connections are sent as connect messages.
					connect(sc, domain, true, null);
				} else {
					r.path = r.path.substring(r.path.indexOf("/") + 2);
					r.path = r.path.substring(r.path.indexOf("/"));
					//Non ssl proxy connections are simply sent directly to the proxy and must be broken down, then replied to as if we were the server.
					connect(sc, domain, false, r.getBytes());
				}
			} catch (IOException e) {
				//				e.printStackTrace();
			}
		}).start();
	}
	
	public void connect(SocketConnector con, String domain, boolean ssl, byte[] extra) {
		try {
			String user = con.client.getInetAddress().getHostAddress();
			boolean canRead = !ssl;
			if (ssl) {
				if (decryptForDomain(domain, user)) {
					canRead = true;
					{//client
						//Pretend the proxy is the server, and do the ssl handshake with the client using a generated certificate to establish the proxy as a man in the middle.
						
						//cache the certificates, generating them is expensive, and there is no need to do so when a site is repeatedly accessed.
						if (!certs.containsKey(domain)) {
							certs.put(domain, CertificateGenerator.generateCertificate(domain, null));
						}
						//Connect to the client as the server.
						KeyStore ks = certs.get(domain);
						
						SSLSocketFactory fact = ProxyServer.factoryFromKeyStore(ks);
						SSLSocket ss = (SSLSocket) fact.createSocket(con.client, null, true);
						
						ss.setUseClientMode(false);
						ss.startHandshake();
						con.client = ss;
					}
					{//server
						//Make an ssl connection to the server.
						SSLSocket ss = (SSLSocket) sslsocketfactory.createSocket(con.server, null, true);
						ss.setUseClientMode(true);
						ss.startHandshake();
						con.server = ss;
					}
				}
			}
			//If the channel is readable, (Either unencrypted or using man in the middle) then the proxy can use http editors and readers on the stream.
			if (canRead) {
				ContentReader cr = readerForDomain(domain, user);
				ContentEditor ce = editorForDomain(domain, user);
				
				//If there are no readers or editors then connect the streams directly.
				if (ce == null && cr == null) {
					con.basicConnect(extra);
				} else {
					if (ce != null) {
						//If there is an editor present, then use full edit/read mode. (the reader possibly being null is ignored)
						con.editingConnect(extra, cr, ce);
					} else {
						//Reader is less invasive than the editor, so if there is no editor don't use one.
						con.readingConnect(extra, cr);
					}
				}
			} else {
				//Proxy can't read the stream, so just connect them.
				con.basicConnect(extra);
			}
		} catch (Exception e) {
			
		}
	}
	
	/**
	 * Determine what websites should be decrypted using a man in the middle.
	 * 
	 * @param domain the domain being accessed
	 * @param user the connected user's ip address
	 * @return if the connection should be decrypted
	 */
	public static boolean decryptForDomain(String domain, String user) {
		if (domain.contains("ios-api-2.duolingo.com")) {
			return true;
		}
		if (domain.contains("quora.com")) {
			return true;
		}
		if (domain.contains("washingtonpost.com")) {
			return true;
		}
		if (domain.contains("smbc-comics.com")) {
			return true;
		}
		return false;
	}
	
	/**
	 * Determine what content reader, if any, should be applied to a stream.
	 * 
	 * @param domain the domain being accessed
	 * @param user the connected user's ip address
	 * @return if the connection should be read and by what
	 */
	public static ContentReader readerForDomain(String domain, String user) {
		return null;
	}
	
	/**
	 * Determine what content editor, if any, should be applied to a stream.
	 * 
	 * @param domain the domain being accessed
	 * @param user the connected user's ip address
	 * @return if the connection should be edited and by what
	 */
	public static ContentEditor editorForDomain(String domain, String user) {
		//doubles points and removes any questions that would make the lesson longer than 10.
		if (domain.contains("duolingo.com")) {
			return Editors.duolingoExtras();
		}
		//removes any questions that would make the lesson longer than 10.
		if (domain.contains("duolingo.com")) {
			return Editors.duolingo();
		}
		//Prevents quora from demanding you log in on related pages.
		if (domain.contains("quora.com")) {
			return Editors.noCookies();
		}
		//Prevents you from reaching the article limit
		if (domain.contains("washingtonpost.com")) {
			return Editors.noCookies();
		}
		return null;
	}
	
	/**
	 * Each ssl socket factory can only use one key store as they are intended to host a single
	 * service, but here they are needed for any website that needs to be decrypted, and a different
	 * certificate is needed for each site. This method simplifies the process of creating new ones.
	 */
	public static SSLSocketFactory factoryFromKeyStore(KeyStore ks) {
		try {
			KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			factory.init(ks, "password".toCharArray());
			
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(factory.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());
			
			return sc.getSocketFactory();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}
		
		public void checkClientTrusted(X509Certificate[] certs, String authType) {}
		
		public void checkServerTrusted(X509Certificate[] certs, String authType) {}
	} };
	static SSLSocketFactory sslsocketfactory;
	static {
		try {
			trust();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void trust() throws Exception {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		sslsocketfactory = sc.getSocketFactory();
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}
	
	//Generated certificate cache
	public static Map<String, KeyStore> certs = new HashMap<>();
	
}

//Class for connecting two sockets together with optional readers and writers.
class SocketConnector {
	
	Socket client;
	Socket server;
	
	/**
	 * @param client the
	 * @param server
	 */
	public SocketConnector(Socket client, Socket server) {
		this.client = client;
		this.server = server;
	}
	
	/**
	 * Connect two sockets together.
	 * 
	 * @param requeue any content that was already read from the client that must still be sent to
	 * the server, or null
	 */
	public void basicConnect(byte[] requeue) {
		new Thread(() -> {
			try {
				InputStream in = client.getInputStream();
				OutputStream out = server.getOutputStream();
				if (requeue != null) {
					out.write(requeue);
				}
				byte[] buffer = new byte[8192];
				int read = 0;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}
			} catch (Exception e) {
				
			} finally {
				try {
					server.close();
					client.close();
				} catch (IOException e) {
				}
			}
		}).start();
		new Thread(() -> {
			try {
				InputStream in = server.getInputStream();
				OutputStream out = client.getOutputStream();
				byte[] buffer = new byte[8192];
				int read = 0;
				while ((read = in.read(buffer)) != -1) {
					
					out.write(buffer, 0, read);
					
				}
			} catch (Exception e) {
				
			} finally {
				try {
					server.close();
					client.close();
				} catch (IOException e) {
				}
			}
		}).start();
	}
	
	/**
	 * Connect two sockets together, and copy the streams to a ContentReader.
	 * 
	 * @param requeue any content that was already read from the client that must still be sent to
	 * the server, or null
	 * @param cr the reader that is to consume a copy of the content
	 */
	public void readingConnect(byte[] requeue, ContentReader cr) {
		readingIn(requeue, cr);
		readingOut(requeue, cr);
		
	}
	
	private void readingIn(byte[] requeue, ContentReader cr) {
		new Thread(() -> {
			try {
				InputStream in = new BufferedInputStream(server.getInputStream());
				OutputStream out = client.getOutputStream();
				ChanneledInputStream clone = new ChanneledInputStream();
				new Thread(() -> {
					try {
						while (true) {
							Response r = UploadTest.readResponse(clone);
							cr.readIncomingHTTP(r);
						}
					} catch (Exception e) {
					}
				}).start();
				
				byte[] buffer = new byte[8192];
				int read = 0;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
					clone.write(buffer, 0, read);
				}
			} catch (Exception e) {
			} finally {
				try {
					server.close();
					client.close();
				} catch (IOException e) {
				}
			}
		}).start();
	}
	
	private void readingOut(byte[] requeue, ContentReader cr) {
		new Thread(() -> {
			try {
				InputStream in = new BufferedInputStream(client.getInputStream());
				OutputStream out = server.getOutputStream();
				ChanneledInputStream clone = new ChanneledInputStream();
				if (requeue != null) {
					out.write(requeue);
					clone.write(requeue, 0, requeue.length);
				}
				new Thread(() -> {
					try {
						while (true) {
							Request r = UploadTest.readRequest(clone);
							cr.readOutgoingHTTP(r);
						}
					} catch (Exception e) {
					}
				}).start();
				
				byte[] buffer = new byte[8192];
				int read = 0;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
					clone.write(buffer, 0, read);
				}
			} catch (Exception e) {
				//				e.printStackTrace();
			} finally {
				try {
					server.close();
					client.close();
				} catch (IOException e) {
				}
			}
		}).start();
	}
	
	private void editingIn(byte[] requeue, ContentReader cr, ContentEditor ce) {
		new Thread(() -> {
			try {
				InputStream in = new BufferedInputStream(server.getInputStream());
				OutputStream out = client.getOutputStream();
				ChanneledInputStream clone = new ChanneledInputStream();
				if (cr != null) new Thread(() -> {
					try {
						while (true) {
							Response r = UploadTest.readResponse(clone);
							cr.readIncomingHTTP(r);
						}
					} catch (Exception e) {
					}
				}).start();
				
				while (true) {
					Response r = UploadTest.readResponse(in);
					ce.editIncomingHTTP(r);
					byte[] b = r.getBytes();
					out.write(b);
					if (cr != null) clone.write(b, 0, b.length);
				}
			} catch (Exception e) {
			} finally {
				try {
					server.close();
					client.close();
				} catch (IOException e) {
				}
			}
		}).start();
	}
	
	private void editingOut(byte[] requeue, ContentReader cr, ContentEditor ce) {
		new Thread(() -> {
			try {
				InputStream in = new BufferedInputStream(client.getInputStream());
				OutputStream out = server.getOutputStream();
				ChanneledInputStream clone = new ChanneledInputStream();
				if (requeue != null) {
					out.write(requeue);
					if (cr != null) clone.write(requeue, 0, requeue.length);
				}
				if (cr != null) new Thread(() -> {
					try {
						while (true) {
							Request r = UploadTest.readRequest(clone);
							cr.readOutgoingHTTP(r);
						}
					} catch (Exception e) {
					}
				}).start();
				
				while (true) {
					Request r = UploadTest.readRequest(in);
					r.gzipOnlyCompression();
					ce.editOutgoingHTTP(r);
					byte[] b = r.getBytes();
					out.write(b);
					if (cr != null) clone.write(b, 0, b.length);
				}
			} catch (Exception e) {
			} finally {
				try {
					server.close();
					client.close();
				} catch (IOException e) {
				}
			}
		}).start();
	}
	
	/**
	 * Connect two sockets together, and allow a ContentEditor to edit the content. Content is also
	 * copied to a ContentReadter if one is provided.
	 * 
	 * @param requeue any content that was already read from the client that must still be sent to
	 * the server, or null
	 * @param cr the reader that is to consume a copy of the content
	 * @param ce the editor that may modify the content
	 */
	public void editingConnect(byte[] requeue, ContentReader cr, ContentEditor ce) {
		if (ce.incoming()) {
			editingIn(requeue, cr, ce);
		} else {
			readingIn(requeue, cr);
		}
		if (ce.outgoing()) {
			editingOut(requeue, cr, ce);
		} else {
			readingOut(requeue, cr);
		}
		
	}
}

/**
 * An interface for custom content editors.
 */
interface ContentEditor {
	
	public void editOutgoingHTTP(Request r);
	
	public void editIncomingHTTP(Response r);
	
	public boolean outgoing();
	
	public boolean incoming();
}

/**
 * An interface for custom content readers.
 */
interface ContentReader {
	
	public void readOutgoingHTTP(Request r);
	
	public void readIncomingHTTP(Response r);
}

/**
 * Adds extra content to the beginning of a stream, to make up for reading too much. Reads only one
 * byte at a time, so it is recommended to wrap the underlying stream in a buffer.
 */
class StreamAppender extends InputStream {
	
	ByteArrayInputStream extra;
	InputStream in;
	
	protected StreamAppender(InputStream in, byte[] extra) {
		this.in = in;
		if (extra != null) this.extra = new ByteArrayInputStream(extra);
	}
	
	public int read() throws IOException {
		if (extra != null) {
			if (extra.available() > 0) {
				return extra.read();
			} else {
				extra = null;
			}
		}
		return in.read();
	}
	
}