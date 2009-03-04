package au.edu.educationau.opensource;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRemoveHTML() {
		assertEquals("hello", WebUtils.removeHTML("hello", false));
		assertEquals("hello", WebUtils.removeHTML("hello", true));
		
		assertEquals("hello", WebUtils.removeHTML("hello<br>", false));
		assertEquals("hello", WebUtils.removeHTML("hello<br>", true));
		assertEquals("hellothere", WebUtils.removeHTML("hello<br>there", false));
		assertEquals("hello there", WebUtils.removeHTML("hello<br>there", true));
		
		assertEquals("Ive", WebUtils.removeHTML("I&#8217;ve", true));
		assertEquals("Ive", WebUtils.removeHTML("I&nbsp;ve", true));
	}

}
