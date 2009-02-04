package au.edu.educationau.opensource.web.keepalive;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 
 * This, in combination with some web-tier changes will keep a user's session alive by polling.
 * 
 * In dwr.xml it should be configured as follows (assuming it is created in 
 * Spring with the bean name "sessionKeepAliveService"):
 * 
 *      <create creator="spring" javascript="SessionKeepAliveService">
 *          <param name="beanName" value="sessionKeepAliveService"/>
 *          <include method="keepAlive"/>
 *      </create>
 * 
 * On the webteir, the following Javascript should be included on every page
 * (either in a <script> tag or via a Javascript file:
 * 
 *  function keepAlive() {
 *		SessionKeepAliveService.keepAlive({errorHandler:function(errorString, exception) {}}); // swallow any errors 
 *		setTimeout("keepAlive()", 150000); // 150000 reload in 2.5 minutes
 *	}
 *
 *	setTimeout("keepAlive()", 2000); 
 * 
 * @author nlothian
 */
public class SessionKeepAliveService {
	private List<KeepAliveEventListener> listeners;
	private Executor listenerExecutor;

	public void setListeners(List<KeepAliveEventListener> listeners) {
		this.listeners = listeners;
	}

	public void setListenerExecutor(Executor listenerExecutor) {
		this.listenerExecutor = listenerExecutor;
	}	
	
	public SessionKeepAliveService() {
		super();

		listenerExecutor = Executors.newCachedThreadPool();
	}

	public String keepAlive(HttpSession session, final HttpServletRequest request) {
		if (listeners != null) {
			ReadOnlySessionValuesCopy sessionCopy = null;
			if (session == null) {
				sessionCopy = new ReadOnlySessionValuesCopy();
				
			} else {
				sessionCopy = new ReadOnlySessionValuesCopy(session);
			}
			sessionCopy.lock();
			
			final ReadOnlySessionValuesCopy finalSessionCopy = sessionCopy;
			
			for (final KeepAliveEventListener listener : listeners) {
				listenerExecutor.execute(new Runnable() {
					public void run() {
						listener.onKeepAlive(finalSessionCopy);
					}
				});
			}
			
		}
		return "alive";
	}

}
