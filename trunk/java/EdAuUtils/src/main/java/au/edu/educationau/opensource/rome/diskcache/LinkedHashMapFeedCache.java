package au.edu.educationau.opensource.rome.diskcache;

import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherException;

public class LinkedHashMapFeedCache implements FeedCache {
	static final Logger logger = Logger.getLogger(LinkedHashMapFeedCache.class);
	
	private final class CacheImpl extends LinkedHashMap<String, CacheInfo> {
		private static final long serialVersionUID = -6977191330127794920L;

		public CacheImpl() {
			super(16, 0.75F, true);
		}
		
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, CacheInfo> eldest) {
			return size() > getMaxEntries();
		}
	}	
	
	private Map<String, CacheInfo> feedCache;
	private int ttlMinutes;
	private int maxEntries = 20;

	public LinkedHashMapFeedCache() {
		feedCache = Collections.synchronizedMap(new CacheImpl());
		ttlMinutes = 30;		
	}

	public int getTtlMinutes() {
		return ttlMinutes;
	}

	public void setTtlMinutes(int ttlMinutes) {
		this.ttlMinutes = ttlMinutes;
	}	
	
	protected boolean cacheHasExpired(CacheInfo cacheInfo) {
		if (cacheInfo == null) {
			return true;
		} else {
			Date now = new Date();	
			long diff = now.getTime() - cacheInfo.getLastCheckedDate().getTime(); // difference in milliseconds
			return (diff >= (ttlMinutes * 60l * 1000l));
		}
	}

	public SyndFeed getFeed(URL url) throws FetcherException {		
		CacheInfo cacheInfo = feedCache.get(url.toExternalForm());
		if (cacheInfo != null) {
			// check if expired
			if (cacheHasExpired(cacheInfo)) {
				// has expired
				// remove from cache
				logger.info("Cache expired for " + url.toExternalForm());				
				feedCache.put(url.toExternalForm(), null);
				return null;
			} else {
				// has not expired
				logger.info("Cache NOT expired for " + url.toExternalForm());		
				// check if is an error
				if (cacheInfo.hasError) {
					logger.warn(url.toExternalForm() + " has errors!");					
					throw new FetcherException(cacheInfo.getErrorMessage());
				}
				return cacheInfo.getFeed();
			}
		} else {
			logger.info("NOT IN CACHE: " + url.toExternalForm());			
			return null;
		}
	}

	public void setFeed(URL url, SyndFeed syndFeed) {
		feedCache.put(url.toExternalForm(), new CacheInfo(syndFeed));		
	}
	
	public void setFeedError(URL url, String error) {
		feedCache.put(url.toExternalForm(), new CacheInfo(error));			
	}	

	public static class CacheInfo implements Serializable {
		private static final long serialVersionUID = 2474473583196872498L;
		private Date lastCheckedDate;
		private SyndFeed feed;
		private boolean hasError;
		private String errorMessage;
		
		public CacheInfo(SyndFeed feed) {
			lastCheckedDate = new Date();
			this.feed = feed;
		}
		
		public CacheInfo(String errorMessage) {
			lastCheckedDate = new Date();
			this.hasError = true;
			this.errorMessage = errorMessage;
		}
		
		public String getErrorMessage() {
			return errorMessage;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public boolean isHasError() {
			return hasError;
		}

		public void setHasError(boolean hasError) {
			this.hasError = hasError;
		}
		
		public SyndFeed getFeed() {
			return feed;
		}
		public void setFeed(SyndFeed feed) {
			this.feed = feed;
		}
		public Date getLastCheckedDate() {
			return (Date) lastCheckedDate.clone();
		}
		public void setLastCheckedDate(Date lastCheckedDate) {
			this.lastCheckedDate = (Date) lastCheckedDate.clone();
		}
	}

	public int getMaxEntries() {
		return maxEntries;
	}

	public void setMaxEntries(int maxSize) {
		this.maxEntries = maxSize;
	}
}
