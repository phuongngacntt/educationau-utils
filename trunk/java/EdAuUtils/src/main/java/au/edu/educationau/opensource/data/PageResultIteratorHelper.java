package au.edu.educationau.opensource.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 
 * A simple way of getting an Iterator from a PageResult DAO method.
 * 
 *  Example:
 *  <pre>
 *	Iterator<Community> communityIterator = PageResultIteratorHelper.iterator(new PageResultIteratorHelper.PageResultGenerator<Community>() {
 *		public PageResult<Community> generate(PageSettings settings) {
 *			return communityDao.findMatchesForUser(finalUser.getSsoUserID(), finalTags, settings);
 *		}							
 *	}, new PageSettings(0, 10));
 *	
 *	while (communityIterator.hasNext()) {
 *		...
 *	}
 *	</pre>
 *			
 *  
 *  
 * 
 * @author nlothian
 *
 */
public class PageResultIteratorHelper {
	public static <U> Iterator<U> iterator(PageResultGenerator<U> generator, PageSettings settings) {
		return new PageResultIterator<U>(generator, settings);
	}
	
	public static interface PageResultGenerator<U> {
		PageResult<U> generate(PageSettings settings);
	}
	
	static class PageResultIterator<U> implements Iterator<U> {		
		
		private PageSettings settings;
		private PageResultGenerator<U> generator;
		
		private PageResult<U> page = null;				
		
		Iterator<U> listItereator = null;
		

		PageResultIterator(PageResultGenerator<U> generator, PageSettings settings) {
			super();
			
			this.generator = generator;
			this.settings = settings;
			
			page = generator.generate(settings);
			listItereator = page.getPageRecords().iterator();			
		}
		
		
		public boolean hasNext() {
			boolean result = false;
			
			synchronized (this) {
				if (listItereator != null) {
					result = listItereator.hasNext();
				}				
				if (result == false) {
					getNextPageIfRequired();
					result = listItereator.hasNext();
				}
			}
			
			return result;
		}

		private void getNextPageIfRequired() {
			synchronized (this) {
				if ((page == null) || (page.getTotalResultsetSize() > (page.getPageSettings().getPageNumber() *  page.getPageSettings().getPageSize()))) {
					// we are not on the final page already, so get the next page
					settings = new PageSettings(settings.getPageNumber() + 1, settings.getPageSize());
					page = generator.generate(settings);
					listItereator = page.getPageRecords().iterator();
				}
			}
		}

		public U next() {			
			if (hasNext() == false) {
				throw new NoSuchElementException();
			}
			return listItereator.next();
		}


		public void remove() {
			throw new UnsupportedOperationException();			
		}
	}
	
}
