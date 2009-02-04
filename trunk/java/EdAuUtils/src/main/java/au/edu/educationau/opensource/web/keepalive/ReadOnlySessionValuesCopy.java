/**
 * 
 */
package au.edu.educationau.opensource.web.keepalive;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;


public class ReadOnlySessionValuesCopy extends HashMap<String, Object> {
	private static final long serialVersionUID = 5165718893054808346L;
	
	private boolean locked = false;

	public ReadOnlySessionValuesCopy() {
		super();
	}
	
	@SuppressWarnings({ "deprecation", "unchecked" })
	public ReadOnlySessionValuesCopy(HttpSession session) {
		this();
		Enumeration<String> names = session.getAttributeNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			this.put(name, session.getAttribute(name));
		}
	}
	
	public void lock() {
		setLocked(true);
	}

	public boolean isLocked() {
		return locked;
	}

	private void setLocked(boolean locked) {
		this.locked = locked;
	}

	@Override
	public Object put(String key, Object value) {
		if (isLocked()) {
			throw new IllegalStateException("This object has been set to read only");
		}
		return super.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		if (isLocked()) {
			throw new IllegalStateException("This object has been set to read only");
		}
		super.putAll(m);
	}
	
	
	
}