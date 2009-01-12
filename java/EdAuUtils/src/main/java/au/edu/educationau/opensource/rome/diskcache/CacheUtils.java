package au.edu.educationau.opensource.rome.diskcache;

import java.io.File;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CacheUtils {
	public static String plainStringToMD5(String input) {
		// Some stuff we use later
		MessageDigest md = null;
		byte[] byteHash = null;
		StringBuffer resultString = new StringBuffer();

		// Bad things can happen here
		try {
			// Choose between MD5 and SHA1
			md = MessageDigest.getInstance("MD5");
			
			// Reset is always good
			md.reset();

			// We really need some conversion here
			md.update(input.getBytes());

			// There goes the hash
			byteHash = md.digest();

			// Now here comes the best part
			for (int i = 0; i < byteHash.length; i++) {
				resultString.append(Integer.toHexString(0xFF & byteHash[i]));
			}

			// That's it!
			return (resultString.toString());			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return null;

	}	
	
	public static String buildCachePath(URL url, String base, String filePrefix) {
    	int hash = url.toString().hashCode();
    	int dir1 = Math.abs(hash % 41);
    	int dir2 = Math.abs(hash % 373);
    	String path = base + File.separator + dir1 + File.separator + dir2;
    	
    	File f = new File(path);
		if (f.exists() && !f.isDirectory()) {
			throw new RuntimeException("Configured cache directory already exists as a file: " + path);
		}	
    	
    	if (!f.exists() && !f.mkdirs()) {
    		throw new RuntimeException("Could not create directory " + path);
    	}
    	
    	
    	String name = plainStringToMD5(url.toExternalForm());
    	
        return path + File.separator + filePrefix + name; 		
		
	}
}
