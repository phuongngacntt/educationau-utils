package au.edu.educationau.opensource.jmx;

import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Needed to extend underlying HttpAdapter to add the annotation, otherwise Spring init errors...
 */
@ManagedResource(objectName = "bean:name=au.edu.educationau.opensource.jmx.HttpAdaptor")
public class HttpAdaptor extends mx4j.tools.adaptor.http.HttpAdaptor {

}
