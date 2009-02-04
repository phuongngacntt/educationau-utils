package au.edu.educationau.opensource.web.keepalive;


public interface KeepAliveEventListener {	
	void onKeepAlive(final ReadOnlySessionValuesCopy copyOfSession);
}
