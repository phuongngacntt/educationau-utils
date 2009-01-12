package au.edu.educationau.opensource.rome.diskcache;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

import com.sun.syndication.fetcher.impl.SyndFeedInfo;

public class DiskBasedFeedInfoCacheTest extends TestCase {

	public void testGetFeedInfo() {
		//fail("Not yet implemented");
	}

	public void testSetFeedInfo() {
		
		try {
			String cacheDir = CacheUtilsTest.getTempCacheDir();
			DiskBasedFeedInfoCache fiCache = new DiskBasedFeedInfoCache();
			fiCache.setCachePath(cacheDir);
			
			String address = "http://www.example.com";
			
			SyndFeedInfo info = new SyndFeedInfo();
		
			info.setUrl(new URL(address));
			info.setETag("random1235");
			
			fiCache.setFeedInfo(new URL(address), info);
			
			SyndFeedInfo returnedInfo = fiCache.getFeedInfo(new URL(address));
			assertNotNull(returnedInfo);
			
		} catch (MalformedURLException e) {			
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}
		
		
	}

	public void testClear() {
		try {
			String cacheDir = CacheUtilsTest.getTempCacheDir();
			DiskBasedFeedInfoCache fiCache = new DiskBasedFeedInfoCache();
			fiCache.setCachePath(cacheDir);
			
			String address = "http://www.example.com";
			
			SyndFeedInfo info = new SyndFeedInfo();
		
			info.setUrl(new URL(address));
			info.setETag("random1235");
			
			fiCache.setFeedInfo(new URL(address), info);
			
			SyndFeedInfo returnedInfo = fiCache.getFeedInfo(new URL(address));
			assertNotNull(returnedInfo);
			
			fiCache.clear();
			
			File cacheDirFile = new File(cacheDir);
			assertTrue(cacheDirFile.exists());
			assertTrue(cacheDirFile.isDirectory());
			assertTrue(cacheDirFile.list().length == 0);
			
		} catch (MalformedURLException e) {			
			e.printStackTrace();
			fail(e.getLocalizedMessage());
		}

	}

	public void testRemove() {
		//fail("Not yet implemented");
	}

}
