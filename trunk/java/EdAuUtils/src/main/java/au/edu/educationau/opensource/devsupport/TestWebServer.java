package au.edu.educationau.opensource.devsupport;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * <p>Use this in development to run a Jetty instance which will use the classes + resources straight out of the source tree (ie. no need to package + redeploy
 * etc. each time a file is changed). The expanded web application will be run from the directory <tt>src/main/webapp</tt> relative to the project root.</p>
 * 
 * <p>It will default to running on port 80, but this can be altered using the <i>test.webserver.port</i> system property.</p> 
 * 
 * <p>Turn SSL on using the <i>test.webserver.usessl</i> system property (true/false), and set the SSL port with the <i>test.webserver.sslport</i>
 * system property (default is 443). If you use SSL you will need to create a key. The best docs for that are 
 * <a href="http://docs.codehaus.org/display/JETTY/How+to+configure+SSL#HowtoconfigureSSL-step4">http://docs.codehaus.org/display/JETTY/How+to+configure+SSL#HowtoconfigureSSL-step4</a>
 * but the easiest way is <br /><br />
 * <code>keytool -keystore keystore -alias jetty -genkey -keyalg RSA</code><br /><br />
 * The key should be deployed to <tt>src/test/resources/keystore</tt> relative to the project root. Passwords for the keystore password,
 * key and truststore all default to <tt>password</tt> but may be specified using the 
 * <tt>test.webserver.ssl.storepassword</tt>, <tt>test.webserver.ssl.keypassword</tt> and <tt>test.webserver.ssl.trustpassword</tt> system properties.
 * </p>
 * 
 * <p>Note that the the web application will be loaded before the webserver ports are set. This means that code in the web application has a 
 * chance to set the relevent system properties. This enables the use of {@link EnvironmentPropertyConfigurer} for configuration.
 * 
 */
public class TestWebServer {
	public static void main(String[] args) throws Exception {
		Server server = new Server(); // don't specify the port here
		
		WebAppContext webappcontext = new WebAppContext();
		webappcontext.setContextPath("/");
		webappcontext.setWar("src/main/webapp");

		HandlerCollection handlers = new HandlerCollection();
		handlers.setHandlers(new Handler[] { webappcontext, new DefaultHandler() });		
		
		server.setHandler(handlers);
		
		// Start the server before adding the connectors
		// This allows the web-app to load, so if system properties are set inside the application they will be available
		server.start();
		
		int port = 80;
		if (System.getProperty("test.webserver.port") != null && Integer.getInteger("test.webserver.port").intValue() > 0) {
			port = Integer.getInteger("test.webserver.port").intValue();
		}
		
		SocketConnector connector = new SocketConnector();
		connector.setPort(port);
		server.addConnector(connector);
		connector.start();
		
		int sslport = 443;
		boolean usingHttps = false;
		if (Boolean.getBoolean("test.webserver.usessl")) {
			
			if (System.getProperty("test.webserver.sslport") != null && Integer.getInteger("test.webserver.sslport").intValue() > 0) {
				sslport = Integer.getInteger("test.webserver.sslport").intValue();
			}			
			
			// Docs on how to generate a keystore are available from http://docs.codehaus.org/display/JETTY/How+to+configure+SSL#HowtoconfigureSSL-step4
			// The easiest way is to use the JDK keytool:
			// 		keytool -keystore keystore -alias jetty -genkey -keyalg RSA
			//
			SslSocketConnector sslConnector = new SslSocketConnector();
			
			sslConnector.setPort(sslport);
			sslConnector.setKeystore("src/test/resources/keystore");
			sslConnector.setPassword(System.getProperty("test.webserver.ssl.storepassword", "password"));
			sslConnector.setKeyPassword(System.getProperty("test.webserver.ssl.keypassword", "password"));
			sslConnector.setTruststore("src/test/resources/keystore");
			sslConnector.setTrustPassword(System.getProperty("test.webserver.ssl.trustpassword", "password"));		
			
			server.addConnector(sslConnector);
			sslConnector.start();
			usingHttps = true;
		}
		
		System.out.println(TestWebServer.class.getName() + " started on port " + port);
		if (usingHttps) {
			System.out.println(TestWebServer.class.getName() + " running https on port " + sslport);
		} else {
			System.out.println(TestWebServer.class.getName() + " https disabled");
		}
		
	}
}
