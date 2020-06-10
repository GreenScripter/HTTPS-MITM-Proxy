import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.regex.Matcher;

public class CertificateGenerator {
	
	static File directory;
	
	static {
		try {
			File f = new File(CertificateGenerator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
			directory = new File(f, "CAroot");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Generate a private key and certificate pair and export them to the given KeyStore. If the key
	 * store is null a new one will be created, otherwise the given one will be returned. Requires a
	 * folder called CAroot with a root certificate named rootCA.crt and a private key named
	 * rootCA.key. The key in the folder named key.key is used to make the certificates. This method
	 * does not currently work for ip addresses, only domain names.
	 * 
	 * @param siteName the name of the site to use
	 * @param addTo the KeyStore to add the key to or null
	 * @return the input key store, or a new one if it was null
	 */
	public static synchronized KeyStore generateCertificate(String siteName, KeyStore addTo) {
		siteName = siteName.replaceAll("[^A-Za-z0-9" + Matcher.quoteReplacement(".-") + "]", "");
		try {
			//Request a signature and generate a key, save both.
			//			runCommand("openssl req -new -nodes -newkey rsa:2048 -keyout " + directory + "/" + siteName + ".key -out " + directory + "/" + siteName + ".csr -days 3650 -subj \"/C=US/ST=SCA/L=SCA/O=Oracle/OU=Java/CN=*." + siteName + "\"").waitFor();
			runCommand("openssl req -new -nodes -key " + directory + "/" + "key.key -out " + directory + "/" + siteName + ".csr -days 3650 -subj \"/C=US/ST=SCA/L=SCA/O=Oracle/OU=Java/CN=*." + siteName + "\"").waitFor();
			//Generate SAN names for the site.
			String altNames = "DNS:" + siteName + ",DNS:www." + siteName + ",DNS:*." + siteName;// + ",DNS:www.*." + siteName + ",DNS:*.*." + siteName;
			File tmpSANConf = new File(directory, "tmpSAN-" + siteName + ".cnf");
			//Write the SAN to a temporary file for openssl to read.
			OutputStream out = new FileOutputStream(tmpSANConf);
			out.write(("[SAN]\nsubjectAltName=" + altNames).getBytes());
			//			out.write("\ndefault_startdate=20150214120000Z".getBytes());
			out.close();
			//Sign the request with the given SANs and the root certificate.
			//Use faketime because openssl makes certificates only valid in the future, while they are needed now. This was causing pages to break in iOS.
			String signRequest = "faketime -f \"-1y\" openssl x509 -req -in " + directory + "/" + siteName + ".csr -CA " + directory + "/" + "rootCA.crt -CAkey " + directory + "/" + "rootCA.key -CAcreateserial -out " + directory + "/" + siteName + ".crt -days 2000 -sha256 -extensions SAN -extfile " + tmpSANConf + "";
			//			System.out.println(signRequest);
			//patch to allow use of homebrew faketime on macs, 
			if (System.getProperty("os.name").toLowerCase().contains("mac")) {
				if (new File("/usr/local/bin/faketime").exists()) signRequest = "/usr/local/bin/" + signRequest;
			}
			
			runCommand(signRequest).waitFor();
			tmpSANConf.delete();
			new File(directory, siteName + ".csr").delete();
			
			//Export the key and certificate to a p12 file.
			String command = "openssl pkcs12 -export -in " + new File(directory, siteName + ".crt") + " -inkey " + new File(directory, "key.key") + " -chain -CAfile " + directory + "/rootCA.crt -name \"*." + siteName + "\" -out " + new File(directory, siteName + ".p12") + " -passin pass:password -passout pass:password";
			
			Process p = runCommand(command);
			p.waitFor();
			
			new File(directory, siteName + ".crt").delete();
			//			new File(directory, siteName + ".key").delete();
			KeyStore ks = KeyStore.getInstance("PKCS12");
			//		ks.load(new FileInputStream(new File("charles-ssl-proxying.p12")), "password".toCharArray());
			ks.load(new FileInputStream(new File(directory, siteName + ".p12")), "password".toCharArray());
			new File(directory, siteName + ".p12").delete();
			System.out.println("Generated key for " + siteName);
			
			if (addTo == null) {
				return ks;
			}
			
			addTo.setKeyEntry("*." + siteName, ks.getKey("*." + siteName, "password".toCharArray()), "password".toCharArray(), ks.getCertificateChain("*." + siteName));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return addTo;
	}
	
	/**
	 * Runs a specified console command
	 * 
	 * @param command the command to run.
	 * @return the command's Process.
	 */
	private static Process runCommand(String command) {
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(new String[] { "bash", "-c", command });
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return process;
	}
	
}
