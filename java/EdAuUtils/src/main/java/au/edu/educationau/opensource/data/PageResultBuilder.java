/**
 * 
 */
package au.edu.educationau.opensource.data;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * 
 * An easy way of creating PageResult-type queries. Note that the SQL from these are probably not as optimized as a hand written version
 * 
 * @author nlothian
 *
 */
public class PageResultBuilder {
		
	@SuppressWarnings("unchecked")
	public static <U> PageResult<U> pageResults(HibernateTemplate hibernateTemplate, final Class[] entityClasses, final String qrySQL, final PageSettings pageSettings, final Map<String, Object> parameters) {
		return (PageResult<U>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				String countSQL = "SELECT COUNT (*) FROM ( " + qrySQL + " ) AS aliasIsReqired"; // postgres insists we need to alias the subquery

				SQLQuery countQry = session.createSQLQuery(countSQL);

				SQLQuery resultsQry = session.createSQLQuery(qrySQL);

				// set parameters on both queries
				if (parameters != null) {						
					Set<Entry<String, Object>> paramsEntrySet = parameters.entrySet();
					for (Entry<String, Object> entry : paramsEntrySet) {
						countQry.setParameter(entry.getKey(), entry.getValue());
						resultsQry.setParameter(entry.getKey(), entry.getValue());
					}
				}
				
				resultsQry.setFirstResult(pageSettings.pageNumber * pageSettings.pageSize);
				resultsQry.setMaxResults(pageSettings.pageSize);
				if (entityClasses != null) {
					for (Class clazz : entityClasses) {
						resultsQry.addEntity(clazz);
					}
				}
									
				BigInteger count = (BigInteger) countQry.uniqueResult();
				List<U> resultList = resultsQry.list();
				
				return new PageResult<U>(resultList, count.longValue(), pageSettings);
			}
			
		});
	}
	
}