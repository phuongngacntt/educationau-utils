package au.edu.educationau.opensource.rome.diskcache;

import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HttpClientFeedFetcher;
import com.sun.syndication.io.FeedException;

public class CachingFeedFetcher extends HttpClientFeedFetcher {
	static final Logger logger = Logger.getLogger(CachingFeedFetcher.class);
	
	private FeedCache feedCache;
	
	public FeedCache getFeedCache() {
		return feedCache;
	}

	public void setFeedCache(FeedCache feedCache) {
		this.feedCache = feedCache;
	}

	public CachingFeedFetcher() {
		super();
		
		addFetcherEventListener(new FetcherEventLogger());		
	}
	
	public CachingFeedFetcher(FeedFetcherCache feedInfoCache, FeedCache feedCache) {
		super(feedInfoCache);		
		
		this.feedCache = feedCache;
	}

	@Override
	public SyndFeed retrieveFeed(URL feedUrl) throws IllegalArgumentException, IOException, FeedException, FetcherException {
		
		SyndFeed result = feedCache.getFeed(feedUrl);
		if (result == null) {
			try {
				result = super.retrieveFeed(feedUrl);
				feedCache.setFeed(feedUrl, result);
			} catch (IllegalArgumentException e) {
				feedCache.setFeedError(feedUrl, e.getLocalizedMessage());
				throw e;
			} catch (IOException e) {
				feedCache.setFeedError(feedUrl, e.getLocalizedMessage());
				throw e;				
			} catch (FeedException e) {
				feedCache.setFeedError(feedUrl, e.getLocalizedMessage());
				throw e;
			} catch (FetcherException e) {
				feedCache.setFeedError(feedUrl, e.getLocalizedMessage());
				throw e;
			}
		}
		
		return result;
	}
}
