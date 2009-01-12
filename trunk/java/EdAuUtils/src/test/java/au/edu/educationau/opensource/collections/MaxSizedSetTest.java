package au.edu.educationau.opensource.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import junit.framework.TestCase;

public class MaxSizedSetTest extends TestCase {

	public void testAdd() {
		int size = 10;
		
		MaxSizedSet<Integer> set = new MaxSizedSet<Integer>(new TreeSet<Integer>(), size);
		assertNotNull(set);
		assertEquals(0, set.size());
		
		// because we always remove the "last" element, it looks better 
		// if we run the loop backwards (with Integers we always remove the highest number)
		for (int i = 20; i > 0; i--) {
			set.add(Integer.valueOf(i));
			assertTrue(set.size() <= size);
			if (i <= size) {
				assertEquals(size, set.size());
			}			
		}
	}

	public void testAddAll() {
		Collection<Integer> col = new ArrayList<Integer>();
		for (int i = 0; i < 20; i++) {
			col.add(Integer.valueOf(i));
		}
		
		int size = 10;
		MaxSizedSet<Integer> set = new MaxSizedSet<Integer>(new TreeSet<Integer>(), size);
		assertNotNull(set);
		assertEquals(0, set.size());
		
		set.addAll(col);
		assertEquals(size, set.size());
		
		
	}

}
