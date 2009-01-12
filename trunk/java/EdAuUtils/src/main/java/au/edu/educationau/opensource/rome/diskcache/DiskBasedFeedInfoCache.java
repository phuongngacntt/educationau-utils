package au.edu.educationau.opensource.rome.diskcache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;

public class DiskBasedFeedInfoCache implements FeedFetcherCache {

	private static final String FILE_PREFIX = "_feedinfo";

	Logger logger = Logger.getLogger(getClass().getName());

	private String cachePath = null;

	protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public DiskBasedFeedInfoCache() {
		cachePath = System.getProperty("java.io.tmpdir") + File.separator + "feedinfocache" + File.separator;
		initCache();
	}

	private synchronized void initCacheLocked() {
		logger.info("Feed Info Cache path set to " + cachePath);
		File f = new File(cachePath);
		if (f.exists() && !f.isDirectory()) {
			throw new RuntimeException("Configured cache directory already exists as a file: " + cachePath);
		}
		if (!f.exists() && !f.mkdirs()) {
			throw new RuntimeException("Could not create directory " + cachePath);
		}
	}
	
	private synchronized void initCache() {
		lock.writeLock().lock();
		try {
			initCacheLocked();			
		} finally {
			lock.writeLock().unlock();
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
		return CacheUtils.buildCachePath(url, cachePath, FILE_PREFIX);
	}

	public SyndFeedInfo getFeedInfo(URL url) {
		lock.readLock().lock();
		try {
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
		} finally {
			lock.readLock().unlock();
		}
	}

	public void setFeedInfo(URL url, SyndFeedInfo feedInfo) {
		lock.readLock().lock();
		try {
			String fileName = buildCachePath(url);
			FileOutputStream fos;
			// need a write lock. Must release read lock before acquiring write lock
			lock.readLock().unlock();
			lock.writeLock().lock();
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
			} finally {
				// downgrade to a read lock
				lock.readLock().lock();
				lock.writeLock().unlock();
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * NOTE: <b>This will lock the cache while it is in progress. It maybe quite slow!</b>
	 */
	public synchronized void clear() {
		logger.info("Clearing feed info cache in " + cachePath);
		lock.writeLock().lock();
		try {
			File cacheDir = new File(this.cachePath);
			if (cacheDir.exists() && cacheDir.isDirectory()) {
				deleteAllCacheFiles(cacheDir);
			}
			
			initCacheLocked(); 			
		} finally {
			lock.writeLock().unlock();
		}
		
		
	}

	private void deleteAllCacheFiles(File cacheDir) {		
		String[] filenames = cacheDir.list();		
		for (String name : filenames) {
			File f = new File(cacheDir.getAbsolutePath() + File.separator + name);
			if (f.isDirectory()) {
				deleteAllCacheFiles(f);
				if (f.list().length == 0) {
					if (!f.delete()) {
						logger.warn("Could not delete directory " + f.getAbsolutePath());
					}
				}				
			} else {
				if (name.startsWith(FILE_PREFIX)) {
					if (!f.delete()) {
						logger.warn("Could not delete file " + f.getAbsolutePath());
					}
				}
			}
		}
		
	}

	public SyndFeedInfo remove(URL feedUrl) {
		lock.readLock().lock();
		try {		
			SyndFeedInfo result = getFeedInfo(feedUrl);
			String fileName = buildCachePath(feedUrl);
			
			lock.readLock().unlock();
			lock.writeLock().lock();
			try {			
				File file = new File(fileName);
				if (file.exists() && !file.delete()) {
					throw new RuntimeException("Could not delete file " + fileName);
				}
		
				return result;
			} finally {
				// downgrade to read lock
				lock.readLock().lock();
				lock.writeLock().unlock();
			}
		} finally {
			lock.readLock().unlock();
		}			
	}
}
