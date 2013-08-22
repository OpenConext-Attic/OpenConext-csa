/*
 * Copyright 2013 SURFnet bv, The Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.surfnet.coin.csa.filter;

import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Filter that goes at great length to grab relevant request and response data and log it using the regular logger.
 *
 */
public class LoggingFilter extends GenericFilterBean {

  private static final Logger REQUEST_LOG = LoggerFactory.getLogger("nl.surfnet.coin.csa.api.request");
  private static final Logger RESPONSE_LOG = LoggerFactory.getLogger("nl.surfnet.coin.csa.api.response");

  public static final String REQUEST_ID_ATTR = "nl.surfnet.coin.csa.filter.LoggingFilter.requestId";
  public static final String OUTPUT_STREAM_COPY_ATTR = "nl.surfnet.coin.csa.filter.LoggingFilter.outputStreamCopy";

  private final Object lock = new Object();

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    // Create copy outpustream and wrap response to use this
    final OutputStream outputStreamCopy = new ByteArrayOutputStream();
    HttpServletResponse wrappedResponse = new MemorizingResponseWrapper((HttpServletResponse) response, outputStreamCopy);

    // Generate unique request id to make correlation request-log and response log easier
    String requestId;
    synchronized (lock) {
      requestId = UUID.randomUUID().toString();
    }

    request.setAttribute(REQUEST_ID_ATTR, requestId);
    request.setAttribute(OUTPUT_STREAM_COPY_ATTR, outputStreamCopy);

    preHandle((HttpServletRequest) request, requestId);
    try {
      chain.doFilter(request, wrappedResponse);
      postHandle(request, wrappedResponse, null);

    } catch (IOException ioe) {
      postHandle(request, wrappedResponse, ioe);
      throw ioe;
    } catch (ServletException se) {
      postHandle(request, wrappedResponse, se);
      throw se;
    } catch (RuntimeException rte) {
      postHandle(request, wrappedResponse, rte);
      throw rte;
    }


  }

  private void preHandle(HttpServletRequest request, String requestId) {
    // Log basic request information at debug level
    if (REQUEST_LOG.isDebugEnabled()) {
      REQUEST_LOG.debug("{} {} {}?{}",
              requestId,
              request.getMethod(),
              request.getRequestURL(),
              request.getQueryString());

      // Log headers with Trace level
      if (REQUEST_LOG.isTraceEnabled()) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
          String headerName = headerNames.nextElement();
          Enumeration headerValues = request.getHeaders(headerName);
          while (headerValues.hasMoreElements()) {
            Object value = headerValues.nextElement();
            REQUEST_LOG.trace("{} Header: {}: {}", requestId, headerName, value);
          }
        }
      }
    }
  }

  private void postHandle(ServletRequest request, HttpServletResponse response, Exception e) {

    String responseData = ((ByteArrayOutputStream) request.getAttribute(OUTPUT_STREAM_COPY_ATTR)).toString();

    MemorizingResponseWrapper responseWrapper = (MemorizingResponseWrapper) response;

    if (RESPONSE_LOG.isDebugEnabled()) {
      String requestId = (String) request.getAttribute(REQUEST_ID_ATTR);
      RESPONSE_LOG.debug("{} Response status: {}",
              requestId,
              responseWrapper.getStatus());

      if (RESPONSE_LOG.isTraceEnabled()) {
        RESPONSE_LOG.trace("{} Response data: {}", requestId, responseData);
      }
    }
    if (e != null) {
      String requestId = (String) request.getAttribute(REQUEST_ID_ATTR);
      RESPONSE_LOG.info("Exception in request {}: ", requestId, e.getMessage());
    }
  }

  /**
   * Response wrapper that keeps the response for logging, as well as the HTTP status.
   */
  public static class MemorizingResponseWrapper extends HttpServletResponseWrapper {

    private final OutputStream outputStream;

    private String status;

    @Override
    public void setStatus(int sc) {
      super.setStatus(sc);
      this.status = String.valueOf(sc);
    }

    @Override
    public void setStatus(int sc, String sm) {
      super.setStatus(sc, sm);
      this.status = sc + " " + sm;
    }
    @Override
    public void sendError(int sc) throws IOException {
      super.sendError(sc);
      status = String.valueOf(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
      super.sendError(sc, msg);
      status = sc + " " + msg;
    }

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @throws IllegalArgumentException
     *          if the response is null
     */
    public MemorizingResponseWrapper(HttpServletResponse response) {
      super(response);
      outputStream = new ByteArrayOutputStream();
    }

    public MemorizingResponseWrapper(HttpServletResponse response, OutputStream outputStream) {
      super(response);
      this.outputStream = outputStream;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return new DelegatingServletOutputStream(
                new TeeOutputStream(super.getOutputStream(), outputStream)
        );
      }

    public String getStatus() {
      return status;
    }
  }
}
