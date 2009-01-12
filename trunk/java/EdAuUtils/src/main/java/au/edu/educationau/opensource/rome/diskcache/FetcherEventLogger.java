package au.edu.educationau.opensource.rome.diskcache;

import org.apache.log4j.Logger;

import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherListener;

public class FetcherEventLogger implements FetcherListener {
	static final Logger logger = Logger.getLogger(FetcherEventLogger.class);
	
	public void fetcherEvent(FetcherEvent event) {		
		String eventType = event.getEventType();
		if (FetcherEvent.EVENT_TYPE_FEED_POLLED.equals(eventType) && logger.isTraceEnabled()) {
			logger.trace(event.getUrlString() + " was polled");			
		} else if (FetcherEvent.EVENT_TYPE_FEED_RETRIEVED.equals(eventType) && logger.isTraceEnabled()) {
			logger.trace(event.getUrlString() + " was retrieved");								
		} else if (FetcherEvent.EVENT_TYPE_FEED_UNCHANGED.equals(eventType) && logger.isTraceEnabled()) {			
			logger.trace(event.getUrlString() + " was unchanged");									
		}
	}

}
