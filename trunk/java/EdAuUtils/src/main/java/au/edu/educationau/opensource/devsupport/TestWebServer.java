package au.edu.educationau.opensource.devsupport;

import java.util.ArrayList;
import java.util.List;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

import au.edu.educationau.opensource.spring20.EnvironmentPropertyConfigurer;

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
		
		WebAppContext firstWebappContext = new WebAppContext();
		firstWebappContext.setContextPath(System.getProperty("test.webserver.webapp.context") != null ? System.getProperty("test.webserver.webapp.context") : "/");
		firstWebappContext.setWar(System.getProperty("test.webserver.webapp.war") != null ? System.getProperty("test.webserver.webapp.war") : "src/main/webapp");
		
		List<Handler> handlerList = new ArrayList<Handler>();
		
		// add extra webapps if configured by properties "test.webserver.webapp2.context" etc.
		for (int i = 2; i < 100; i++) {
			if (System.getProperty("test.webserver.webapp" + i + ".context") != null) {
				WebAppContext extraWebappContext = new WebAppContext();
				extraWebappContext.setContextPath(System.getProperty("test.webserver.webapp" + i + ".context"));
				if (System.getProperty("test.webserver.webapp" + i + ".war") == null) {
					throw new RuntimeException("No war folder specified for webapp " + i);
				}
				extraWebappContext.setWar(System.getProperty("test.webserver.webapp" + i + ".war"));
				handlerList.add(extraWebappContext);
			}
		}
		
		// NOTE: the reason the "first" context is in the list after the extra ones, is because it's usually "/" context - 
		// if it was first in the list, Jetty won't let other contexts register (i.e. "/" will swallow all requests) 
		handlerList.add(firstWebappContext);
		handlerList.add(new DefaultHandler());
		
		HandlerCollection handlerCollection = new HandlerCollection();
		handlerCollection.setHandlers(handlerList.toArray(new Handler[0]));		
		
		server.setHandler(handlerCollection);
		
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
