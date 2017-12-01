import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import eu.splender.utils.Compressor;

public class BurpExtender implements IBurpExtender {

	private static boolean debug = false;
	private static boolean persist = false;

	public BurpExtender() {
	}

	public void setCommandLineArgs(String[] args) {

		if (args[0] != null) {
			if (args[0].equalsIgnoreCase("debug")) {
				debug = true;
			}
		}

		if (args[1] != null) {
			if (args[1].equalsIgnoreCase("persist")) {
				persist = true;
			}
		}
	};

	public byte[] processProxyMessage(int messageReference, boolean messageIsRequest, String remoteHost, int remotePort,
			boolean serviceIsHttps, String httpMethod, String url, String resourceType, String statusCode,
			String responseContentType, byte[] message, int[] action) {

		if (messageIsRequest)
			System.out.println("[INFO] Inspecting request message for URL: " + url);
		else
			System.out.println("[INFO] Inspecting response message");

		String strMessage = new String(message);

		String[] strHeadersAndContent = strMessage.split("\\r\\n\\r\\n");

		String headers = strHeadersAndContent[0];
		if (debug)
			System.out.println("[DEBUG] Message header: " + headers);

		try {
			Scanner s = new Scanner(headers);
			int contentLength = 0;
			String contentEncoding = "";

			while (s.hasNext() && (contentLength == 0 || contentEncoding.equals(""))) {
				String line = s.next();
				if (line.startsWith("Content-Length")) {
					line = s.next();
					contentLength = Integer.parseInt(line);
					if (debug)
						System.out.println("[DEBUG] Content-Length: " + contentLength);
				}
				if (line.startsWith("Content-Encoding")) {
					line = s.next();
					contentEncoding = line.trim();
					if (debug)
						System.out.println("[DEBUG] Content-Encoding: " + contentEncoding);
				}
			}
			s.close();

			if (contentLength == 0) {
				if (debug)
					System.out.println("[DEBUG] Content length could not be determined");
				return message;
			}
			if (contentEncoding.equals("jzlib")) {
				String inflatedContent = Compressor.decompressToString(
						Arrays.copyOfRange(message, message.length - contentLength, message.length));
				if (persist) {
					long timestamp = System.currentTimeMillis();
					String type = (messageIsRequest ? "request" : "response");
					BufferedWriter out = null;
					try {
						out = new BufferedWriter(new FileWriter(timestamp + "-" + type + ".xml"));
						out.write(inflatedContent);
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						out.close();
						if (debug)
							System.out.println("[DEBUG] Request/Response written to file");
					}
				}
			}
		} catch (Exception e) {
			System.out.println("[INFO] nothing to decompress");
		}
		return message;
	}

	public void registerHttpRequestMethod(java.lang.reflect.Method makeHttpRequestMethod,
			Object makeHttpRequestObject) {
	}

	public void applicationClosing() {
	}
}
