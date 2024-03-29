package au.edu.educationau.opensource.spring20;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * <p>Extends {@link PropertyPlaceholderConfigurer} with support for using different configurations for different deployment environments.<p>
 * 
 * <p>Base property values will be read from <tt>environment/application.properties</tt> in the classpath. Override properties will then
 * be loaded from file named with the <tt>value of the <em>application.environment</em> system property-application.properties</tt></p>
 * 
 * <p>For example, if <tt>application.environment</tt> is set to <tt>dev</tt> then <tt>environment/dev-application.properties</tt> will be loaded.</p>
 * 
 * <p>The <tt>application.instance</tt> system property can be set for an additional level of override.</p>
 * 
 * <p>Any file may contain special properties with the prefix of <tt>system.</tt>. These properties will have <tt>system.</tt> removed, and will
 * then be made available as system properties. For example: 
 * <br /><br />
 * <code>system.some.property=value</code>
 * <br /><br />
 * means that <code>System.getProperty("some.property");</code> will return <em>value</em>.
 * <p>
 *  
 * @author nlothian
 * @author bsmyth
 *
 */
public class EnvironmentPropertyConfigurer extends	PropertyPlaceholderConfigurer {
	static final Logger logger = Logger.getLogger(EnvironmentPropertyConfigurer.class);
	
	private static final String DEFAULT_LOCATION = "classpath:environment/application.properties";		
	
	private String defaultResourceLocation = DEFAULT_LOCATION;
	
	public String getDefaultResourceLocation() {
		return defaultResourceLocation;
	}
	
	
	public EnvironmentPropertyConfigurer(String defaultResourceLocation) {
		this.defaultResourceLocation = defaultResourceLocation;
		
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		
		Resource defaultResource = resourceLoader.getResource(getDefaultResourceLocation()); 

		String environment = System.getProperty("application.environment");
		String instance = System.getProperty("application.instance");

		Resource environmentProps = null;
		Resource instanceProps = null;	
		if (environment != null) {
			logger.info("application.environment system property is set to " + environment);
			environmentProps = resourceLoader.getResource("classpath:environment/"	+ environment + "-application.properties");	
			if (instance != null) {
				logger.info("application.instance system property is set to " + instance);
				String filename = "classpath:environment/"	+ instance + "-" + environment + "-application.properties";
				try {
					instanceProps = resourceLoader.getResource(filename);
				} catch (Exception e) {
					logger.warn("Could not load property file " + filename + " so no instance properties will be used");
				}
			}
		} else {
			logger.warn("no application.environment system property is set. Default location (" + getDefaultResourceLocation() + ") will be used for properties." );
		}

		if (environmentProps != null) {
			Resource[] resources = null;
			if (instanceProps != null) {
				logger.info("Setting property resource locations to: " + defaultResource + ", " + environmentProps + " and " + instanceProps);
				resources = new Resource[] {
								defaultResource, // use default					
								environmentProps, // override with specified environment		
								instanceProps // override with specified instance
							};
			} else {
				logger.info("Setting property resource locations to: " + defaultResource + " and " + environmentProps);
				resources = new Resource[] {
						defaultResource, // use default					
						environmentProps // override with specified environment		
					};				
			}
			
			setLocations(resources);			
			
		} else {
			logger.info("Setting property resource locations to: " + defaultResource);
			setLocation(defaultResource); // if none provided use default
		}		
	}
	
	public EnvironmentPropertyConfigurer() {
		this(DEFAULT_LOCATION);
	}


	@Override
	protected void loadProperties(Properties properties) throws IOException {		
		super.loadProperties(properties);
		
		// for each property that begins with "system" set it as a system property (dropping the "system" prefix)
		for (Object propertyName : properties.keySet()) {		
			int systemIndex = ((String)propertyName).indexOf("system"); 
			
			if (systemIndex >=0 ) {
				System.setProperty(((String)propertyName).substring(systemIndex + "system.".length()), properties.getProperty((String)propertyName));
				if (logger.isTraceEnabled()) {
					logger.trace("System property " + ((String)propertyName).substring(systemIndex + "system.".length()) + " set to " + properties.getProperty((String)propertyName));
				}				
			}
		}
		
	}

}