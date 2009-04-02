package au.edu.educationau.opensource.servlets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Use linking to a resource somehow like
 * 
 * <p>
 * /static/cacheable/css/<%= ControllerUtils.cacheControlfile(request, "/styles/reset.css") %>,<%= ControllerUtils.cacheControlfile(request,
 * "/styles/global.css") %>
 * 
 * <p>
 * then in web.xml
 * 
 * <p>
 * <servlet-mapping> <servlet-name>FileCombiner</servlet-name> <url-pattern>/combine/*</url-pattern> </servlet-mapping>
 * 
 * <p>
 * then in urlrewrite.xml
 * 
 * <p>
 * <rule> <from>^/static/cacheable/css/(.+)$</from> <to>/combine/?cachethis=1&amp;filenames=$1&amp;contenttype=text/css</to> </rule>
 * 
 * <p>
 * <rule> <from>^/static/cacheable/js/(.+)$</from> <to>/combine/?cachethis=1&amp;filenames=$1&amp;contenttype=text/javascript</to> </rule>
 * 
 * <p>
 * if you have styles resources under a nested path (eg /something/styles/doot.css), use a / to delimit the parent dirs and then ~ after that
 * 
 * <p>
 * eg
 * 
 * <p>
 * <%= ControllerUtils.cacheControlfile(request, "/themes~styles~branded_global.css") %>
 * 
 * @author nlothian
 * @author bsmyth
 */
public class FileCombinerServlet extends HttpServlet {

	private static final String LAST_MODIFIED_HEADER = "Last-modified";

	private static final long serialVersionUID = -2818467139725459646L;

	private static final String BAD_BROWSERS_REGEX = ".*MSIE 6\\..*";

	private static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";

	private Pattern pattern = Pattern.compile(BAD_BROWSERS_REGEX);

	private Pattern timestampedUrlPattern = Pattern.compile("^/stmp/(.+?)/(.*)");

	private String cacheUntil;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		this.cacheUntil = config.getInitParameter("cache-until");
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if (request.getParameter("contenttype") != null) {
			response.setContentType(request.getParameter("contenttype"));
		}

		String filenames = request.getParameter("filenames");

		Long maxLastModified = getMaximumModifiedDate(filenames.split(","));
		if (maxLastModified != null) {
			// header isn't provided in milliseconds so deal with that
			maxLastModified = maxLastModified / 1000 * 1000;
		}
		
		long requestedLastModified = request.getDateHeader(HEADER_IF_MODIFIED_SINCE);
				
		if (requestedLastModified == -1 || maxLastModified > requestedLastModified) {
			
			// cache has old version, so we will combine and compress
			
			HttpServletResponse theResponse = response;
			// check if the browser claims to accept gzipped content
			String ae = request.getHeader("accept-encoding");
			String ua = request.getHeader("User-Agent");
			if (ua != null && ae != null && ae.indexOf("gzip") != -1) {
				// now make sure it isn't IE 6.x (which doesn't work properly with compressed javascript, (technically SP2 does, but too bad))
				Matcher matcher = pattern.matcher(ua);
				if (!matcher.find()) {
					theResponse = new GZIPResponseWrapper(response);
				}
			}
			
			boolean useFarFutureCacheExpiry = (request.getParameter("cachethis") != null);
			if (useFarFutureCacheExpiry && (cacheUntil != null)) {
				response.addHeader("Expires", cacheUntil);
			}

			if (filenames != null) {				
				String[] filenameArray = filenames.split(",");
				
				for (String name : filenameArray) {
					if (!name.startsWith("/")) {
						name = "/" + name;
					}					
					name = name.replaceAll("~", "/"); // TODO: remove when ~ isn't used for path component delimeter by anything anymore
					
					Matcher matcher = timestampedUrlPattern.matcher(name);
					if (matcher.matches()) {
						name = matcher.group(2);
					}	
					
					if (!name.startsWith("/")) {
						name = "/" + name;
					}
					RequestDispatcher rd = getServletContext().getRequestDispatcher(name);
					if (rd != null) {
						rd.include(request, theResponse);	
						try {
							theResponse.getOutputStream().println("\n");
						} catch (IllegalStateException e) {
							// getWriter() has already been called
							theResponse.getWriter().println("\n");
						}
					} else {
						getServletContext().log(this.getClass().getName() + ": Error: Could not find resource " + name);
					}
				}
			}

			if (maxLastModified != null) {
				theResponse.setDateHeader(LAST_MODIFIED_HEADER, maxLastModified);
			}

			if (theResponse instanceof GZIPResponseWrapper) {
				((GZIPResponseWrapper) theResponse).finishResponse();
			}

		} else {
			
			// cache has current version
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			
		}
	}
	
	private Long getModifiedDate(String filename) {

		Long lastModified = null;

		if (!filename.startsWith("/")) {
			filename = "/" + filename;
		}

		Matcher matcher = timestampedUrlPattern.matcher(filename);
		if (matcher.matches()) {
			try {
				lastModified = Long.parseLong(matcher.group(1).replace("/", ""));
			} catch (Exception e) {
				// swallow
			}
		}

		return lastModified;

	}

	private Long getMaximumModifiedDate(String[] filenames) {

		Long maxLastModified = null;

		for (String name : filenames) {

			Long currentLastModified = getModifiedDate(name);

			if (maxLastModified == null || currentLastModified > maxLastModified) {
				maxLastModified = currentLastModified;
			}

		}

		return maxLastModified;
	}

	class GZIPResponseWrapper extends HttpServletResponseWrapper {
		protected HttpServletResponse origResponse = null;

		protected ServletOutputStream stream = null;

		protected PrintWriter writer = null;

		public GZIPResponseWrapper(HttpServletResponse response) {
			super(response);
			origResponse = response;
		}

		public ServletOutputStream createOutputStream() throws IOException {
			return (new GZIPResponseStream(origResponse));
		}

		public void finishResponse() {
			try {
				if (writer != null) {
					writer.close();
				} else {
					if (stream != null) {
						stream.close();
					}
				}
			} catch (IOException e) {
				getServletContext().log("error occured closing response");
			}
		}

		public void flushBuffer() throws IOException {
			stream.flush();
		}

		public ServletOutputStream getOutputStream() throws IOException {
			if (writer != null) {
				throw new IllegalStateException("getWriter() has already been called!");
			}
			if (stream == null)
				stream = createOutputStream();
			return (stream);
		}

		public PrintWriter getWriter() throws IOException {
			if (writer != null) {
				return (writer);
			}
			if (stream != null) {
				throw new IllegalStateException("getOutputStream() has already been called!");
			}
			stream = createOutputStream();
			writer = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));
			return (writer);
		}

		public void setContentLength(int length) {
		}
	}

	class GZIPResponseStream extends ServletOutputStream {
		protected ByteArrayOutputStream baos = null;

		protected GZIPOutputStream gzipstream = null;

		protected boolean closed = false;

		protected HttpServletResponse response = null;

		protected ServletOutputStream output = null;

		public GZIPResponseStream(HttpServletResponse response) throws IOException {
			super();
			closed = false;
			this.response = response;
			this.output = response.getOutputStream();
			baos = new ByteArrayOutputStream();
			gzipstream = new GZIPOutputStream(baos);
		}

		public void close() throws IOException {
			if (closed) {
				throw new IOException("This output stream has already been closed");
			}
			gzipstream.finish();
			byte[] bytes = baos.toByteArray();
			response.addHeader("Content-Length", Integer.toString(bytes.length));
			response.addHeader("Content-Encoding", "gzip");
			output.write(bytes);
			output.flush();
			output.close();
			closed = true;
		}

		public void flush() throws IOException {
			if (closed) {
				throw new IOException("Cannot flush a closed output stream");
			}
			gzipstream.flush();
		}

		public void write(int b) throws IOException {
			if (closed) {
				throw new IOException("Cannot write to a closed output stream");
			}
			gzipstream.write((byte) b);
		}

		public void write(byte b[]) throws IOException {
			write(b, 0, b.length);
		}

		public void write(byte b[], int off, int len) throws IOException {
			if (closed) {
				throw new IOException("Cannot write to a closed output stream");
			}
			gzipstream.write(b, off, len);
		}

		public boolean closed() {
			return (this.closed);
		}

		public void reset() {
			// noop
		}
	}

}
