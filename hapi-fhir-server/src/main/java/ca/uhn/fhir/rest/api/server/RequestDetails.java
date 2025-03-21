/*
 * #%L
 * HAPI FHIR - Server Framework
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.rest.api.server;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.PreferHeader;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.server.IRestfulServerDefaults;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.util.StopWatch;
import ca.uhn.fhir.util.UrlUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class RequestDetails {

	public static final byte[] BAD_STREAM_PLACEHOLDER =
			(Msg.code(2543) + "PLACEHOLDER WHEN READING FROM BAD STREAM").getBytes(StandardCharsets.UTF_8);
	private final StopWatch myRequestStopwatch;
	private IInterceptorBroadcaster myInterceptorBroadcaster;
	private String myTenantId;
	private String myCompartmentName;
	private String myCompleteUrl;
	private String myFhirServerBase;
	private IIdType myId;
	private String myOperation;
	private Map<String, String[]> myParameters;
	private byte[] myRequestContents;
	private String myRequestPath;
	private RequestTypeEnum myRequestType;
	private String myResourceName;
	private boolean myRespondGzip;
	private IRestfulResponse myResponse;
	private RestOperationTypeEnum myRestOperationType;
	private String mySecondaryOperation;
	private boolean mySubRequest;
	private Map<String, List<String>> myUnqualifiedToQualifiedNames;
	private Map<Object, Object> myUserData;
	private IBaseResource myResource;
	private String myRequestId;
	private String myTransactionGuid;
	private String myFixedConditionalUrl;
	private boolean myRewriteHistory;
	private int myMaxRetries;
	private boolean myRetry;

	/**
	 * Constructor
	 */
	public RequestDetails(IInterceptorBroadcaster theInterceptorBroadcaster) {
		myInterceptorBroadcaster = theInterceptorBroadcaster;
		myRequestStopwatch = new StopWatch();
	}

	/**
	 * Copy constructor
	 */
	public RequestDetails(RequestDetails theRequestDetails) {
		myInterceptorBroadcaster = theRequestDetails.getInterceptorBroadcaster();
		myRequestStopwatch = theRequestDetails.getRequestStopwatch();
		myTenantId = theRequestDetails.getTenantId();
		myCompartmentName = theRequestDetails.getCompartmentName();
		myCompleteUrl = theRequestDetails.getCompleteUrl();
		myFhirServerBase = theRequestDetails.getFhirServerBase();
		myId = theRequestDetails.getId();
		myOperation = theRequestDetails.getOperation();
		myParameters = theRequestDetails.getParameters();
		myRequestContents = theRequestDetails.getRequestContentsIfLoaded();
		myRequestPath = theRequestDetails.getRequestPath();
		myRequestType = theRequestDetails.getRequestType();
		myResourceName = theRequestDetails.getResourceName();
		myRespondGzip = theRequestDetails.isRespondGzip();
		myResponse = theRequestDetails.getResponse();
		myRestOperationType = theRequestDetails.getRestOperationType();
		mySecondaryOperation = theRequestDetails.getSecondaryOperation();
		mySubRequest = theRequestDetails.isSubRequest();
		myUnqualifiedToQualifiedNames = theRequestDetails.getUnqualifiedToQualifiedNames();
		myUserData = theRequestDetails.getUserData();
		myResource = theRequestDetails.getResource();
		myRequestId = theRequestDetails.getRequestId();
		myTransactionGuid = theRequestDetails.getTransactionGuid();
		myFixedConditionalUrl = theRequestDetails.getFixedConditionalUrl();
	}

	public String getFixedConditionalUrl() {
		return myFixedConditionalUrl;
	}

	public void setFixedConditionalUrl(String theFixedConditionalUrl) {
		myFixedConditionalUrl = theFixedConditionalUrl;
	}

	public String getRequestId() {
		return myRequestId;
	}

	public void setRequestId(String theRequestId) {
		myRequestId = theRequestId;
	}

	public StopWatch getRequestStopwatch() {
		return myRequestStopwatch;
	}

	/**
	 * Returns the request resource (as provided in the request body) if it has been parsed.
	 * Note that this value is only set fairly late in the processing pipeline, so it
	 * may not always be set, even for operations that take a resource as input.
	 *
	 * @since 4.0.0
	 */
	public IBaseResource getResource() {
		return myResource;
	}

	/**
	 * Sets the request resource (as provided in the request body) if it has been parsed.
	 * Note that this value is only set fairly late in the processing pipeline, so it
	 * may not always be set, even for operations that take a resource as input.
	 *
	 * @since 4.0.0
	 */
	public void setResource(IBaseResource theResource) {
		myResource = theResource;
	}

	public void addParameter(String theName, String[] theValues) {
		getParameters();
		myParameters.put(theName, theValues);
	}

	protected abstract byte[] getByteStreamRequestContents();

	/**
	 * Return the charset as defined by the header contenttype. Return null if it is not set.
	 */
	public abstract Charset getCharset();

	public String getCompartmentName() {
		return myCompartmentName;
	}

	public void setCompartmentName(String theCompartmentName) {
		myCompartmentName = theCompartmentName;
	}

	public String getCompleteUrl() {
		return myCompleteUrl;
	}

	public void setCompleteUrl(String theCompleteUrl) {
		myCompleteUrl = theCompleteUrl;
	}

	/**
	 * Returns the <b>conditional URL</b> if this request has one, or <code>null</code> otherwise. For an
	 * update or delete method, this is the part of the URL after the <code>?</code>. For a create, this
	 * is the value of the <code>If-None-Exist</code> header.
	 *
	 * @param theOperationType The operation type to find the conditional URL for
	 * @return Returns the <b>conditional URL</b> if this request has one, or <code>null</code> otherwise
	 */
	@SuppressWarnings("EnumSwitchStatementWhichMissesCases")
	public String getConditionalUrl(RestOperationTypeEnum theOperationType) {
		if (myFixedConditionalUrl != null) {
			return myFixedConditionalUrl;
		}
		switch (theOperationType) {
			case CREATE:
				String retVal = this.getHeader(Constants.HEADER_IF_NONE_EXIST);
				if (isBlank(retVal)) {
					return null;
				}
				if (retVal.startsWith(this.getFhirServerBase())) {
					retVal = retVal.substring(this.getFhirServerBase().length());
				}
				return retVal;
			case DELETE:
			case UPDATE:
			case PATCH:
				if (this.getId() != null && this.getId().hasIdPart()) {
					return null;
				}

				int questionMarkIndex = this.getCompleteUrl().indexOf('?');
				if (questionMarkIndex == -1) {
					return null;
				}

				return this.getResourceName() + this.getCompleteUrl().substring(questionMarkIndex);
			default:
				return null;
		}
	}

	/**
	 * Returns the HAPI FHIR Context associated with this request
	 */
	public abstract FhirContext getFhirContext();

	/**
	 * The fhir server base url, independant of the query being executed
	 *
	 * @return the fhir server base url
	 */
	public String getFhirServerBase() {
		return myFhirServerBase;
	}

	public void setFhirServerBase(String theFhirServerBase) {
		myFhirServerBase = theFhirServerBase;
	}

	public abstract String getHeader(String name);

	public abstract List<String> getHeaders(String name);

	/**
	 * Adds a new header
	 *
	 * @param theName The header name
	 * @param theValue The header value
	 * @since 7.2.0
	 */
	public abstract void addHeader(String theName, String theValue);

	/**
	 * Replaces any existing header(s) with the given name using a List of new header values
	 *
	 * @param theName The header name
	 * @param theValue The header value
	 * @since 7.2.0
	 */
	public abstract void setHeaders(String theName, List<String> theValue);

	public IIdType getId() {
		return myId;
	}

	public void setId(IIdType theId) {
		myId = theId;
	}

	/**
	 * Returns the attribute map for this request. Attributes are a place for user-supplied
	 * objects of any type to be attached to an individual request. They can be used to pass information
	 * between interceptor methods.
	 */
	public abstract Object getAttribute(String theAttributeName);

	/**
	 * Returns the attribute map for this request. Attributes are a place for user-supplied
	 * objects of any type to be attached to an individual request. They can be used to pass information
	 * between interceptor methods.
	 */
	public abstract void setAttribute(String theAttributeName, Object theAttributeValue);

	/**
	 * Retrieves the body of the request as binary data. Either this method or {@link #getReader} may be called to read
	 * the body, not both.
	 *
	 * @return a {@link InputStream} object containing the body of the request
	 * @throws IllegalStateException if the {@link #getReader} method has already been called for this request
	 * @throws IOException           if an input or output exception occurred
	 */
	public abstract InputStream getInputStream() throws IOException;

	public String getOperation() {
		return myOperation;
	}

	public void setOperation(String theOperation) {
		myOperation = theOperation;
	}

	public Map<String, String[]> getParameters() {
		if (myParameters == null) {
			myParameters = new HashMap<>();
		}
		return Collections.unmodifiableMap(myParameters);
	}

	public void setParameters(Map<String, String[]> theParams) {
		myParameters = theParams;
		myUnqualifiedToQualifiedNames = null;

		// Sanitize keys if necessary to prevent injection attacks
		boolean needsSanitization = false;
		for (String nextKey : theParams.keySet()) {
			if (UrlUtil.isNeedsSanitization(nextKey)) {
				needsSanitization = true;
				break;
			}
		}
		if (needsSanitization) {
			myParameters = myParameters.entrySet().stream()
					.collect(
							Collectors.toMap(t -> UrlUtil.sanitizeUrlPart((String) ((Map.Entry<?, ?>) t).getKey()), t ->
									(String[]) ((Map.Entry<?, ?>) t).getValue()));
		}
	}

	/**
	 * Retrieves the body of the request as character data using a <code>BufferedReader</code>. The reader translates the
	 * character data according to the character encoding used on the body. Either this method or {@link #getInputStream}
	 * may be called to read the body, not both.
	 *
	 * @return a <code>Reader</code> containing the body of the request
	 * @throws UnsupportedEncodingException if the character set encoding used is not supported and the text cannot be decoded
	 * @throws IllegalStateException        if {@link #getInputStream} method has been called on this request
	 * @throws IOException                  if an input or output exception occurred
	 * @see jakarta.servlet.http.HttpServletRequest#getInputStream
	 */
	public abstract Reader getReader() throws IOException;

	/**
	 * Returns an invoker that can be called from user code to advise the server interceptors
	 * of any nested operations being invoked within operations. This invoker acts as a proxy for
	 * all interceptors
	 */
	public IInterceptorBroadcaster getInterceptorBroadcaster() {
		return myInterceptorBroadcaster;
	}

	/**
	 * The part of the request URL that comes after the server base.
	 * <p>
	 * Will not contain a leading '/'
	 * </p>
	 */
	public String getRequestPath() {
		return myRequestPath;
	}

	public void setRequestPath(String theRequestPath) {
		assert theRequestPath.length() == 0 || theRequestPath.charAt(0) != '/';
		myRequestPath = theRequestPath;
	}

	public RequestTypeEnum getRequestType() {
		return myRequestType;
	}

	public void setRequestType(RequestTypeEnum theRequestType) {
		myRequestType = theRequestType;
	}

	public String getResourceName() {
		return myResourceName;
	}

	public void setResourceName(String theResourceName) {
		myResourceName = theResourceName;
	}

	public IRestfulResponse getResponse() {
		return myResponse;
	}

	public void setResponse(IRestfulResponse theResponse) {
		this.myResponse = theResponse;
	}

	public RestOperationTypeEnum getRestOperationType() {
		return myRestOperationType;
	}

	public void setRestOperationType(RestOperationTypeEnum theRestOperationType) {
		myRestOperationType = theRestOperationType;
	}

	public String getSecondaryOperation() {
		return mySecondaryOperation;
	}

	public void setSecondaryOperation(String theSecondaryOperation) {
		mySecondaryOperation = theSecondaryOperation;
	}

	public abstract IRestfulServerDefaults getServer();

	/**
	 * Returns the server base URL (with no trailing '/') for a given request
	 *
	 * @deprecated Use {@link #getFhirServerBase()} instead. Deprecated in HAPI FHIR 7.0.0
	 */
	@Deprecated
	public abstract String getServerBaseForRequest();

	/**
	 * Gets the tenant ID associated with the request. Note that the tenant ID
	 * and the partition ID are not the same thing - Depending on the specific
	 * partition interceptors in use, the tenant ID might be used internally
	 * to derive the partition ID or it might not. Do not assume that it will
	 * be used for this purpose.
	 */
	public String getTenantId() {
		return myTenantId;
	}

	/**
	 * Sets the tenant ID associated with the request. Note that the tenant ID
	 * and the partition ID are not the same thing - Depending on the specific
	 * partition interceptors in use, the tenant ID might be used internally
	 * to derive the partition ID or it might not. Do not assume that it will
	 * be used for this purpose.
	 */
	public void setTenantId(String theTenantId) {
		myTenantId = theTenantId;
	}

	public Map<String, List<String>> getUnqualifiedToQualifiedNames() {
		if (myUnqualifiedToQualifiedNames == null) {
			for (String next : myParameters.keySet()) {
				for (int i = 0; i < next.length(); i++) {
					char nextChar = next.charAt(i);
					if (nextChar == ':' || nextChar == '.') {
						if (myUnqualifiedToQualifiedNames == null) {
							myUnqualifiedToQualifiedNames = new HashMap<>();
						}
						String unqualified = next.substring(0, i);
						List<String> list =
								myUnqualifiedToQualifiedNames.computeIfAbsent(unqualified, k -> new ArrayList<>(4));
						list.add(next);
						break;
					}
				}
			}
		}

		if (myUnqualifiedToQualifiedNames == null) {
			myUnqualifiedToQualifiedNames = Collections.emptyMap();
		}

		return myUnqualifiedToQualifiedNames;
	}

	/**
	 * Returns a map which can be used to hold any user specific data to pass it from one
	 * part of the request handling chain to another. Data in this map can use any key, although
	 * user code should try to use keys which are specific enough to avoid conflicts.
	 * <p>
	 * A new map is created for each individual request that is handled by the server,
	 * so this map can be used (for example) to pass authorization details from an interceptor
	 * to the resource providers, or for example to pass data from a hook method
	 * on the {@link ca.uhn.fhir.interceptor.api.Pointcut#SERVER_INCOMING_REQUEST_POST_PROCESSED}
	 * to a later hook method on the {@link ca.uhn.fhir.interceptor.api.Pointcut#SERVER_OUTGOING_RESPONSE}
	 * pointcut.
	 * </p>
	 */
	public Map<Object, Object> getUserData() {
		if (myUserData == null) {
			myUserData = new HashMap<>();
		}
		return myUserData;
	}

	public boolean isRespondGzip() {
		return myRespondGzip;
	}

	public void setRespondGzip(boolean theRespondGzip) {
		myRespondGzip = theRespondGzip;
	}

	/**
	 * Is this request a sub-request (i.e. a request within a batch or transaction)? This
	 * flag is used internally by hapi-fhir-jpaserver-base, but not used in the plain server
	 * library. You may use it in your client code as a hint when implementing transaction logic in the plain
	 * server.
	 * <p>
	 * Defaults to {@literal false}
	 * </p>
	 */
	public boolean isSubRequest() {
		return mySubRequest;
	}

	/**
	 * Is this request a sub-request (i.e. a request within a batch or transaction)? This
	 * flag is used internally by hapi-fhir-jpaserver-base, but not used in the plain server
	 * library. You may use it in your client code as a hint when implementing transaction logic in the plain
	 * server.
	 * <p>
	 * Defaults to {@literal false}
	 * </p>
	 */
	public void setSubRequest(boolean theSubRequest) {
		mySubRequest = theSubRequest;
	}

	public final synchronized byte[] loadRequestContents() {
		if (myRequestContents == null) {
			// Initialize the byte array to a non-null value to avoid repeated calls to getByteStreamRequestContents()
			// which can occur when getByteStreamRequestContents() throws an Exception
			myRequestContents = ArrayUtils.EMPTY_BYTE_ARRAY;
			try {
				myRequestContents = getByteStreamRequestContents();
			} finally {
				if (myRequestContents == null) {
					// if reading the stream throws an exception, then our contents are still null, but the stream is
					// dead.
					// Set a placeholder value so nobody tries to read again.
					myRequestContents = BAD_STREAM_PLACEHOLDER;
				}
			}
			assert myRequestContents != null : "We must not re-read the stream.";
		}
		return getRequestContentsIfLoaded();
	}

	/**
	 * Returns the request contents if they were loaded, returns <code>null</code> otherwise
	 *
	 * @see #loadRequestContents()
	 */
	public byte[] getRequestContentsIfLoaded() {
		return myRequestContents;
	}

	public void removeParameter(String theName) {
		Validate.notNull(theName, "theName must not be null");
		getParameters();
		myParameters.remove(theName);
	}

	/**
	 * This method may be used to modify the contents of the incoming
	 * request by hardcoding a value which will be used instead of the
	 * value received by the client.
	 * <p>
	 * This method is useful for modifying the request body prior
	 * to parsing within interceptors. It generally only has an
	 * impact when called in the {@link IServerInterceptor#incomingRequestPostProcessed(RequestDetails, HttpServletRequest, HttpServletResponse)}
	 * method
	 * </p>
	 */
	public void setRequestContents(byte[] theRequestContents) {
		myRequestContents = theRequestContents;
	}

	public String getTransactionGuid() {
		return myTransactionGuid;
	}

	public void setTransactionGuid(String theTransactionGuid) {
		myTransactionGuid = theTransactionGuid;
	}

	public boolean isRewriteHistory() {
		return myRewriteHistory;
	}

	public void setRewriteHistory(boolean theRewriteHistory) {
		myRewriteHistory = theRewriteHistory;
	}

	public int getMaxRetries() {
		return myMaxRetries;
	}

	public void setMaxRetries(int theMaxRetries) {
		myMaxRetries = theMaxRetries;
	}

	public boolean isRetry() {
		return myRetry;
	}

	public void setRetry(boolean theRetry) {
		myRetry = theRetry;
	}

	public boolean isPreferAsync() {
		String prefer = getHeader(Constants.HEADER_PREFER);
		PreferHeader preferHeader = RestfulServerUtils.parsePreferHeader(prefer);
		return preferHeader.getRespondAsync();
	}
}
