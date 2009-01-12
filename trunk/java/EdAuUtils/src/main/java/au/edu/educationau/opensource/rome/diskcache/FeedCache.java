package au.edu.educationau.opensource.rome.diskcache;

import java.net.URL;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherException;

public interface FeedCache {
	public SyndFeed getFeed(URL url) throws FetcherException;
	public void setFeed(URL url, SyndFeed syndFeed);
	public void setFeedError(URL url, String error);
}
