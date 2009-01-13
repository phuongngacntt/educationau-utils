package au.edu.educationau.opensource.data;

import java.io.Serializable;


/**
 * Encapsulates page request settings, used with DAO methods and included in {@link PageResult} beans for UI display of page links. 
 */
public class PageSettings implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2698368860316127805L;

	/** 
	 * 0 is the index for the first page 
	 **/
	public int pageNumber;
	
	/** The number of records per page */
	public int pageSize;
	
	/** Optional, only used if relevant to the query */
	public String sortBy;

	/** Only used if {@link #sortBy} is used */
	public boolean sortAscending;


	public PageSettings(int pageNumber, int pageSize) {
		super();
		this.pageSize = pageSize;
		this.pageNumber = pageNumber;
	}
	
	public PageSettings(int pageNumber, int pageSize, String sortBy, boolean sortAscending) {
		this(pageNumber,pageSize);
		
		this.sortBy = sortBy;
		this.sortAscending = sortAscending;
	}
	

	public int getPageNumber() {
		return pageNumber;
	}

	public int getPageSize() {
		return pageSize;
	}
	
	public String getSortBy() {
		return sortBy;
	}
	
	public boolean isSortAscending() {
		return sortAscending;
	}	
}