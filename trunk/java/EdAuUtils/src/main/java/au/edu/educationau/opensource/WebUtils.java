package au.edu.educationau.opensource;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class WebUtils {
	/** Strips entities and tags from a string, optionally replacing the tags with a space */
    public static String removeHTML(String str, boolean addSpace) {
        return str == null ? "" : str.replaceAll("&(#*).{2,4};", "").replaceAll("(?s)<.*?>", addSpace ? " " : "").trim();
    }
    
	/**
	 * (This method was lifted from {@link org.springframework.web.servlet.view.AbstractView#exposeModelAsRequestAttributes(Map, HttpServletRequest)})
	 *
	 * Expose the model objects in the given map as request attributes.
	 * Names will be taken from the model Map.
	 * This method is suitable for all resources reachable by {@link javax.servlet.RequestDispatcher}.
	 * @param model Map of model objects to expose
	 * @param request current HTTP request
	 */
	public static void exposeModelAsRequestAttributes(Map<String, Object> model, HttpServletRequest request) {
		Iterator<Map.Entry<String, Object>> it = model.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> entry = it.next();
			if (!(entry.getKey() instanceof String)) {
				throw new IllegalArgumentException(
						"Invalid key [" + entry.getKey() + "] in model Map: only Strings allowed as model keys");
			}
			String modelName = (String) entry.getKey();
			Object modelValue = entry.getValue();
			if (modelValue != null) {
				request.setAttribute(modelName, modelValue);
			}
			else {
				request.removeAttribute(modelName);
			}
		}
	}
	
	/**
	 * Replaces strings of the form ${parameterName} within the given string with the corresponding value from the given
	 * set of parameters for that parameter name. The parameter values are treated as literals, not regexes.
	 *
	 * i.e. "select * from ${table} where id = ${id}" would have ${table} and ${id} replaced with the values for "table" and "id"
	 * in the given parameter map.
	 *
	 * The modified string is returned.
	 */
	public static String substituteParameters(String templateString, Map<String, Object> parameters) {
		for (String parameter : parameters.keySet()) {
			templateString = templateString.replaceAll("\\$\\{" + parameter + "\\}", Matcher.quoteReplacement(parameters.get(parameter).toString()));
		}
		if (templateString.matches(".*\\$\\{\\w+?\\}.*")) {
			throw new RuntimeException("Unsubstituted symbol in string: " + templateString);
		}
		return templateString;
	}
	
	public static boolean redirectToHttps(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if ("http".equalsIgnoreCase(request.getScheme())) {
			// make sure we have a session created under http.
			// https sessions cannot be read under http, but the http sessions can be read under https
			HttpSession session = request.getSession(true);			
			
			StringBuffer url = request.getRequestURL();
			url.insert(4, 's');
			if (request.getQueryString() != null) {
				url.append("?");
				url.append(request.getQueryString());
			}
			
			response.sendRedirect(url.toString());
			
			return true;
		} else {
			return false;
		}
	}	
    
}
