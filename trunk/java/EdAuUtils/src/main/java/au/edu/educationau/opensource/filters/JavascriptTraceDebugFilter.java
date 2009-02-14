package au.edu.educationau.opensource.filters;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.Logger;

/**
 * IE 6/7 don't report which file an error occurs in (only line number, which can be misleading sometimes also) - this filter injects logging code onto every
 * line (that ends with a semicolon and optionally whitespace) which logs the current file/line number to a div (anchored to top-left corner of page). This way
 * it becomes more obvious which file/line is failing.
 * 
 * NOTE this will break JavaScript files that use chained if/else without braces (FF will give error about "invalid character").
 * 
 * It will also fail to log any lines that execute before document.body is populated (i.e. skips logging if document.body == null).
 * 
 * Sample web.xml config below:
 *
 * 	<filter>
 *		<filter-name>JavascriptTraceDebugFilter</filter-name>
 *		<filter-class>au.edu.educationau.opensource.filters.JavascriptTraceDebugFilter</filter-class>
 *		<init-param>
 *			<param-name>includesRegex</param-name>
 *			<param-value>.*</param-value>
 *		</init-param>
 *		<init-param>
 *			<param-name>excludesRegex</param-name>
 *			<param-value>__effectively_disabled__</param-value>
 *		</init-param>
 *	</filter>
 *
 * <!-- Make sure this goes after any gzip filters (so it runs first before being gzipped) -->
 *	<filter-mapping>
 *		<filter-name>JavascriptTraceDebugFilter</filter-name>
 *		<url-pattern>*.js</url-pattern>
 *		<dispatcher>REQUEST</dispatcher>
 *		<dispatcher>FORWARD</dispatcher>
 *		<dispatcher>INCLUDE</dispatcher>
 *	</filter-mapping>
 */
public class JavascriptTraceDebugFilter implements Filter {
	private static final Logger LOG = Logger.getLogger(JavascriptTraceDebugFilter.class);

	private Pattern includesRegex;

	private Pattern excludesRegex;
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// on one line so it doesn't disturb line numbers in real source
		String jsLoggingFunction = "var eduAuLogger;if (eduAuLogger == null) {eduAuLogger = function(text){if (document.body) {var lineNumberDiv = document.getElementById(\"lineNumberDiv\");if (lineNumberDiv == null) {lineNumberDiv = document.createElement(\"div\");lineNumberDiv.id = \"lineNumberDiv\";document.body.appendChild(lineNumberDiv);lineNumberDiv.style.cssText = \"position: absolute; top: 0; left: 0; width: 250px; height: 250px; text-align: left; background-color: white; overflow: scroll; opacity: 0.75\";}var lineDiv = document.createElement(\"div\");lineDiv.innerHTML = text;lineNumberDiv.appendChild(lineDiv);}}}";

		class FakeServletResponse extends HttpServletResponseWrapper {

			private ByteArrayServletOutputStream outputStream = new ByteArrayServletOutputStream();

			private PrintWriter printWriter = new PrintWriter(outputStream);

			public FakeServletResponse(HttpServletResponse response) {
				super(response);
			}

			@Override
			public ServletOutputStream getOutputStream() throws IOException {
				return outputStream;
			}

			@Override
			public PrintWriter getWriter() throws IOException {
				return printWriter;
			}

			@Override
			public void setContentLength(int len) {
			}

			public String getOutputAsString() {
				printWriter.flush(); // necessary - an auto flushing PrintWriter doesn't autoflush on write methods, which may be called by the compiled JSP
				return new String(outputStream.toByteArray());
			}

			class ByteArrayServletOutputStream extends ServletOutputStream {
				ByteArrayOutputStream underlyingOutputStream = new ByteArrayOutputStream();

				@Override
				public void write(int b) throws IOException {
					underlyingOutputStream.write(b);
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					underlyingOutputStream.write(b, off, len);
				}

				public byte[] toByteArray() {
					return underlyingOutputStream.toByteArray();
				}
			}
		}

		boolean insertLoggingCode = true;

		if (includesRegex != null) {
			insertLoggingCode = includesRegex.matcher(((HttpServletRequest) request).getRequestURI()).matches();
			if (!insertLoggingCode) {
				LOG.info("Resource didn't match the includesRegex: " + ((HttpServletRequest) request).getRequestURI());
			}
		}

		if (insertLoggingCode && excludesRegex != null) {
			insertLoggingCode = !excludesRegex.matcher(((HttpServletRequest) request).getRequestURI()).matches();
			if (!insertLoggingCode) {
				LOG.info("Excluding resource as it matched the excludesRegex: " + ((HttpServletRequest) request).getRequestURI());
			}
		}

		if (insertLoggingCode) {
			LOG.info("Inserting logging into resource: " + ((HttpServletRequest) request).getRequestURI());

			FakeServletResponse fakeResponse = new FakeServletResponse((HttpServletResponse) response);
			chain.doFilter(request, fakeResponse);

			String javascriptText = fakeResponse.getOutputAsString();
			javascriptText = jsLoggingFunction + javascriptText;

			StringBuilder b = new StringBuilder();
			BufferedReader bufferedReader = new BufferedReader(new StringReader(javascriptText));

			String line = null;
			for (int i = 1; (line = bufferedReader.readLine()) != null; i++) {
				if (line.matches(".*;\\s*$")) {
					line += "eduAuLogger(\"" + ((HttpServletRequest) request).getRequestURI() + ":" + i + "\");";
				}
				b.append(line + "\r\n");
			}

			response.getOutputStream().write(b.toString().getBytes());
		} else {
			chain.doFilter(request, response);
		}
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		String includesRegexString = filterConfig.getInitParameter("includesRegex");
		if (includesRegexString != null) {
			includesRegex = Pattern.compile(includesRegexString);
		}

		String excludesRegexString = filterConfig.getInitParameter("excludesRegex");
		if (excludesRegexString != null) {
			excludesRegex = Pattern.compile(excludesRegexString);
		}
	}

	public void destroy() {
	}
}
