package au.edu.educationau.opensource.rome.diskcache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherException;

public class DiskFeedCache extends LinkedHashMapFeedCache {
	Logger logger = Logger.getLogger(getClass().getName());

	private volatile String cachePath = null;
	private volatile long diskCacheHits;
	private volatile long memoryCacheHits;
	private volatile long cacheMisses;
	private volatile long cacheExpiries;

	protected volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private Map<String, SoftReference<CacheInfo>> memCache = Collections.synchronizedMap(new HashMap<String, SoftReference<CacheInfo>>());

	public DiskFeedCache() {
		cachePath = System.getProperty("java.io.tmpdir") + File.separator + "feedinfo" + File.separator;
		initCache();

	}

	/**
	 * MUST be called ONLY when lock.writeLock().lock() has been called
	 */	
	private void initCacheLocked() {
		logger.info("Feed Cache path set to " + cachePath);
		File f = new File(cachePath);
		if (f.exists() && !f.isDirectory()) {
			throw new RuntimeException("Configured cache directory already exists as a file: " + cachePath);
		}

		if (!f.exists() && !f.mkdirs()) {
			throw new RuntimeException("Could not create directory " + cachePath);
		}
	}
	
	private void initCache() {
		lock.writeLock().lock();
		try {	
			initCacheLocked();
		} finally {
			lock.writeLock().unlock();
		}
	}
	

	public long getMemoryCacheHits() {
		return memoryCacheHits;
	}

	public long getCacheExpiries() {
		return cacheExpiries;
	}

	public long getDiskCacheHits() {
		return diskCacheHits;
	}

	public long getCacheMisses() {
		return cacheMisses;
	}

	public String getCachePath() {
		lock.readLock().lock();
		try {			
			return cachePath;
		} finally {
			lock.readLock().unlock();
		}		
	}

	public void setCachePath(String cachePath) {
		lock.writeLock().lock();
		try {			
			this.cachePath = cachePath;
			initCacheLocked();
		} finally {
			lock.writeLock().unlock();
		}			
	}

	public void resetCacheInfo() {
		diskCacheHits = 0;
		cacheMisses = 0;
		cacheExpiries = 0;
	}

	protected String buildCachePath(URL url) {
		return CacheUtils.buildCachePath(url, cachePath, "_feed");		
	}

	private CacheInfo getFromCache(URL url) throws IOException, ClassNotFoundException {
		lock.readLock().lock();
		try {
			CacheInfo cacheInfo = getFromMemCache(url);
			if (cacheInfo == null) {
				String fileName = buildCachePath(url);
				File file = new File(fileName);
				if (file.exists()) {
					diskCacheHits++;
					FileInputStream fis = null;
					try {
						fis = new FileInputStream(file);
						ObjectInputStream ois = new ObjectInputStream(fis);
						cacheInfo = (CacheInfo) ois.readObject();
					} finally {
						if (fis != null) {
							try {
								fis.close();
							} catch (IOException e) {
								logger.warn("error closing file", e);
							}
						}
					}
				}
			} else {
				memoryCacheHits++;
			}

			return cacheInfo;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public SyndFeed getFeed(URL url) throws FetcherException {		
		try {
			CacheInfo cacheInfo = getFromCache(url);
			if (cacheInfo == null) {
				cacheMisses++;
				// not in cache yet
				logger.info("cache miss: " + url.toExternalForm());
				return null;
			} else {
				if (cacheHasExpired(cacheInfo)) {
					// has expired
					// remove old version
					removeFromCache(url);

					logger.info("cache expired: " + url.toExternalForm());
					cacheExpiries++;
					return null;
				} else {
					if (logger.isTraceEnabled()) {
						logger.trace("cache not expired for " + url.toExternalForm());
					}

					// not expired
					addToMemCache(url, cacheInfo);
					if (cacheInfo.isHasError()) {
						logger.info(url.toExternalForm() + " has cached errors!");
						throw new FetcherException("cached " + cacheInfo.getErrorMessage());
					}
					return cacheInfo.getFeed();
				}
			}
		} catch (IOException e) {
			logger.error("Attempting to read from cache", e);
			throw new FetcherException("Attempting to read from cache", e);
		} catch (ClassNotFoundException e) {
			logger.error("Attempting to read from cache", e);
			throw new FetcherException("Attempting to read from cache", e);
		} 
	}

	private void addToMemCache(URL url, CacheInfo cacheInfo) {
		// this does not need to use the lock, because memCache is synchronized
		memCache.put(url.toExternalForm(), new SoftReference<CacheInfo>(cacheInfo));
	}

	private CacheInfo getFromMemCache(URL url) {
		// this does not need to use the lock, because memCache is synchronized
		SoftReference<CacheInfo> sr = memCache.get(url.toExternalForm());
		if (sr != null) {
			CacheInfo info = sr.get(); // may return null if has been collected
			if (info == null) {
				memCache.remove(url.toExternalForm());
			}
			return info;
		} else {
			return null;
		}
	}

	private void removeFromCache(URL url) {
		lock.readLock().lock();
		try {
			String fileName = buildCachePath(url);
			File file = new File(fileName);
			lock.readLock().unlock();
			lock.writeLock().lock();
			try {
				if (file.exists() && !file.delete()) {
					throw new RuntimeException("Could not delete file " + fileName);
				}
				// remove from memory cache
				memCache.remove(url.toExternalForm());
			} finally {
				// downgrade the lock
				lock.readLock().lock();
				lock.writeLock().unlock();				
			}
		} finally {
			lock.readLock().unlock();
		}			
	}

	@Override
	public void setFeedError(URL url, String error) {		
		try {
			logger.info("Caching error for " + url.toExternalForm() + " (Error msg is " + error + ")");
			CacheInfo cacheInfo = getFromCache(url);
			if (cacheInfo == null) {				
				lock.writeLock().lock();
				try {
				
					// not already in cache
					CacheInfo info = new CacheInfo(error);
					addToMemCache(url, info);
	
					String fileName = buildCachePath(url);
					FileOutputStream fos = null;
					try {
						fos = new FileOutputStream(fileName);
						ObjectOutputStream oos = new ObjectOutputStream(fos);
						oos.writeObject(info);
						fos.flush();
					} finally {
						try {
							if (fos != null) {
								fos.close();
							}
						} catch (IOException e) {
							logger.warn("error closing file", e);
						}
					}
				} finally {
					lock.writeLock().unlock();					
				}
			} else {
				logger.info("Error for " + url.toExternalForm() + " already cached");
			}
		} catch (Exception e) {
			logger.error("Error writing to cache for " + url.toExternalForm(), e);
		}	
	}

	@Override
	public void setFeed(URL url, SyndFeed syndFeed) {
		lock.readLock().lock();
		try {
			CacheInfo info = new CacheInfo(syndFeed);
	
			addToMemCache(url, info);
	
			String fileName = buildCachePath(url);
			FileOutputStream fos = null;
			lock.readLock().unlock();
			lock.writeLock().lock();
			try {
				fos = new FileOutputStream(fileName);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(info);
				fos.flush();
			} catch (Exception e) {
				// Error writing to cache is fatal
				logger.error("Error writing cache", e);
				throw new RuntimeException("Attempting to write to cache", e);
			} finally {
				try {
					if (fos != null) {
						fos.close();
					}
				} catch (IOException e) {
					logger.warn("error closing file", e);
				}
				// downgrade the lock				
				lock.readLock().lock();
				lock.writeLock().unlock();				
			}
		} finally {
			lock.readLock().unlock();
		}			
	}

}
