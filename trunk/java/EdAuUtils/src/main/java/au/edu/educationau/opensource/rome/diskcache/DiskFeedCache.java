package au.edu.educationau.opensource.rome.diskcache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherException;

public class DiskFeedCache extends LinkedHashMapFeedCache {
	Logger logger = Logger.getLogger(getClass().getName());
	
    private String cachePath = null;    
    private long diskCacheHits;
    private long memoryCacheHits;
    private long cacheMisses;
    private long cacheExpiries;
    
    private Map<String, SoftReference<CacheInfo>> memCache = new HashMap<String, SoftReference<CacheInfo>>();

	public DiskFeedCache()  {
		cachePath = System.getProperty("java.io.tmpdir") + File.separator + "feedinfo" + File.separator;
		initCache();
        
    }    
    
	private void initCache() {
		logger.info("Feed Cache path set to " + cachePath);
		File f = new File(cachePath);
		if (f.exists() && !f.isDirectory()) {
			throw new RuntimeException("Configured cache directory already exists as a file: " + cachePath);
		}		
		
    	if (!f.exists() && !f.mkdirs()) {
    		throw new RuntimeException("Could not create directory " + cachePath);
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
		return cachePath;
	}

	public void setCachePath(String cachePath) {
		this.cachePath = cachePath;
		initCache();
	}
	
	public synchronized void resetCacheInfo() {
		diskCacheHits = 0;
		cacheMisses = 0;
		cacheExpiries = 0;
	}
	
	protected String buildCachePath(URL url) {
    	return CacheUtils.buildCachePath(url, cachePath, "_feed");
    }
    
	private CacheInfo getFromCache(URL url) throws IOException, ClassNotFoundException {
		CacheInfo cacheInfo = getFromMemCache(url);
		if (cacheInfo == null) {
			String fileName =  buildCachePath(url);
			File file = new File(fileName);
			if (file.exists()) {
				diskCacheHits++;				
				FileInputStream fis = null;
				try {
    		        fis = new FileInputStream(file);
    		        ObjectInputStream ois = new ObjectInputStream(fis);
    		        cacheInfo = (CacheInfo)ois.readObject();					
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
    	memCache.put(url.toExternalForm(), new SoftReference<CacheInfo>(cacheInfo));
    }

    private CacheInfo getFromMemCache(URL url) {
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
		String fileName =  buildCachePath(url);
		File file = new File(fileName);
		if (file.exists() && !file.delete()) {
			throw new RuntimeException("Could not delete file " + fileName);			
		}
		// remove from memory cache
		memCache.remove(url.toExternalForm());
	}

	@Override
	public void setFeedError(URL url, String error) {
		try {
			logger.info("Caching error for " + url.toExternalForm() + " (Error msg is " + error + ")");		
			CacheInfo cacheInfo = getFromCache(url);
			if (cacheInfo == null) {
				// not already in cache
				CacheInfo info = new CacheInfo(error);
				addToMemCache(url, info);
				
				String fileName =  buildCachePath(url);
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
			} else {
				logger.info("Error for " + url.toExternalForm() + " already cached");
			}
		} catch (Exception e) {
			logger.error("Error writing to cache for " + url.toExternalForm(), e);
		}
	}
	
	
	@Override
	public void setFeed(URL url, SyndFeed syndFeed) {
		CacheInfo info = new CacheInfo(syndFeed);
		
		addToMemCache(url, info);
		
		String fileName =  buildCachePath(url);
        FileOutputStream fos = null;
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
        }
	}

}
