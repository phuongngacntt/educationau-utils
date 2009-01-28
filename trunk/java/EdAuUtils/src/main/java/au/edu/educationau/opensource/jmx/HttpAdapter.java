package au.edu.educationau.opensource.jmx;

import mx4j.tools.adaptor.http.HttpAdaptor;

import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Needed to extend underlying HttpAdapter to add the annotation, otherwise Spring init errors...
 */
@ManagedResource(objectName = "bean:name=au.edu.educationau.myedna.jmx.HttpAdapter")
public class HttpAdapter extends HttpAdaptor {

}
