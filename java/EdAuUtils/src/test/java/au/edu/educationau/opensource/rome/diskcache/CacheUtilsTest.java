package au.edu.educationau.opensource.rome.diskcache;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;

public class CacheUtilsTest extends TestCase {

	public void testPlainStringToMD5() {
		
		String input = "input";		
		String result = CacheUtils.plainStringToMD5(input);
		assertNotNull(input);
		assertTrue(!input.equals(result));

		input = "";		
		result = CacheUtils.plainStringToMD5(input);
		assertNotNull(input);
		assertTrue(!input.equals(result));
		
		
		try {
			input = null;		
			CacheUtils.plainStringToMD5(input);
			fail("No exception was thrown for null input");
		} catch (NullPointerException e) {
			// should always be caught
			assertTrue(1 == 1);
		}
		
	}
	
	public static String getTempCacheDir() {
		String tmpDir = System.getProperty("java.io.tmpdir");
		if (!tmpDir.endsWith(File.separator)) {
			tmpDir = tmpDir + File.separator;
		}
		
		// create timestampted dirname
		String cacheDir = tmpDir + "tstCache" + Long.toString(System.currentTimeMillis()) + File.separator;

		System.err.println("Using " + cacheDir + " for caching");
		
		File cacheDirFile = new File(cacheDir);
		cacheDirFile.deleteOnExit();
		
		return cacheDir;
	}

	public void testBuildCachePath() {
		String cacheDir = getTempCacheDir();
		File cacheDirFile = new File(cacheDir);
		
		String address = "http://www.example.com";

		try {
			// get the name a new file should be called
			String newFilename = CacheUtils.buildCachePath(new URL(address), cacheDir, "tst");
			File f = new File(newFilename);
			
			assertTrue(newFilename, !f.exists()); // the file should not exist
			
			// the cache directory should now be created
			assertTrue(cacheDir, cacheDirFile.exists());
			assertTrue(cacheDir, cacheDirFile.isDirectory());	
			assertTrue(cacheDir, cacheDirFile.canRead());			
			assertTrue(cacheDir, cacheDirFile.canWrite());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}

}
