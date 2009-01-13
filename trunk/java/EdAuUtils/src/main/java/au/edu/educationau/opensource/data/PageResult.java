package au.edu.educationau.opensource.data;

import java.io.Serializable;
import java.util.List;


/**
 * Returned by pagination-aware DAO queries to encapsulate the normal List<T> result, as 
 * well as the total size of the resultset (for displaying page links on the UI) and the pagination settings the page was retrieved with.  
 */
public class PageResult<T> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 407018460606503194L;

	/** A list of beans that represent the page (e.g. will usually be 10 in size) */
	private List<T> pageRecords;

	/** The unbounded count of the query used to retrieve the page - this would be the number of records retrieved if the page size was unlimited */
	private long totalResultsetSize;
	
	/** The settings used to create the page - this is used on the UI to display page links */
	private PageSettings pageSettings;

	public PageResult(List<T> pageRecords, long totalResultsetSize, PageSettings pageSettings) {
		this.pageRecords = pageRecords;
		this.totalResultsetSize = totalResultsetSize;
		this.pageSettings = pageSettings;
	}
	
	public long getTotalPageCount() {
		return (totalResultsetSize/pageSettings.pageSize) + ((totalResultsetSize % pageSettings.pageSize)==0 ? 0 : 1);
	}
	

	public List<T> getPageRecords() {
		return pageRecords;
	}

	public long getTotalResultsetSize() {
		return totalResultsetSize;
	}
	
	public PageSettings getPageSettings() {
		return pageSettings;
	}
	
	public int getFirstIncludedRowIndex() {
		return pageSettings.getPageNumber() * pageSettings.getPageSize();
	}
	
	public int getLastIncludedRowIndex() {
		return getFirstIncludedRowIndex() + pageRecords.size();
	}
	
}