package au.edu.educationau.opensource.rome.diskcache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;

import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;

@ManagedResource(objectName="bean:name=network.FeedInfoCache")
public class DiskBasedFeedInfoCache implements FeedFetcherCache {

	Logger logger = Logger.getLogger(getClass().getName());

	private String cachePath = null;

	public DiskBasedFeedInfoCache() {
		cachePath = System.getProperty("java.io.tmpdir") + File.separator + "feedinfocache" + File.separator;
		initCache();
	}

	private void initCache() {
		logger.info("Feed Info Cache path set to " + cachePath);
		File f = new File(cachePath);
		if (f.exists() && !f.isDirectory()) {
			throw new RuntimeException("Configured cache directory already exists as a file: " + cachePath);
		}
    	if (!f.exists() && !f.mkdirs()) {
    		throw new RuntimeException("Could not create directory " + cachePath);
    	}		
	}
	
	public String getCachePath() {
		return cachePath;
	}

	public void setCachePath(String cachePath) {
		this.cachePath = cachePath;
		initCache();
	}

	protected String buildCachePath(URL url) {
		return CacheUtils.buildCachePath(url, cachePath, "_feedinfo");
	}

	public SyndFeedInfo getFeedInfo(URL url) {
		SyndFeedInfo info = null;
		String fileName = buildCachePath(url);
		FileInputStream fis;
		try {
			fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			info = (SyndFeedInfo) ois.readObject();
			fis.close();
		} catch (FileNotFoundException fnfe) {
			logger.debug("Cache miss for " + url.toString());
		} catch (ClassNotFoundException cnfe) {
			// Error writing to cache is fatal
			logger.error("Attempting to read from cache", cnfe);
			throw new RuntimeException("Attempting to read from cache", cnfe);
		} catch (IOException fnfe) {
			// Error writing to cache is fatal
			logger.error("Attempting to read from cache", fnfe);
			throw new RuntimeException("Attempting to read from cache", fnfe);
		}
		if (info == null) {
			logger.info("Cache miss for url " + url.toExternalForm());
		} 
		return info;
	}

	public void setFeedInfo(URL url, SyndFeedInfo feedInfo) {
		String fileName = buildCachePath(url);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(fileName);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(feedInfo);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			// Error writing to cache is fatal
			logger.error("Error writing cache", e);
			throw new RuntimeException("Attempting to write to cache", e);
		}
	}

	public synchronized void clear() {
		throw new IllegalStateException(this.getClass() +  " does not support clearing the cache");
		
	}

	public SyndFeedInfo remove(URL feedUrl) {
		SyndFeedInfo result = getFeedInfo(feedUrl);
		
		String fileName =  buildCachePath(feedUrl);
		File file = new File(fileName);
		if (file.exists() && !file.delete()) {			
			throw new RuntimeException("Could not delete file " + fileName);			
		}
		
		return result;
	}
}
