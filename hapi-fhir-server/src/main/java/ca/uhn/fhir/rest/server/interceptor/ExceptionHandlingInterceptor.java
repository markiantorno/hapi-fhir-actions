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
package ca.uhn.fhir.rest.server.interceptor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.api.server.IRestfulResponse;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import ca.uhn.fhir.rest.server.method.BaseResourceReturningMethodBinding;
import ca.uhn.fhir.rest.server.servlet.ServletRestfulResponse;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.HttpHeaders.CONTENT_ENCODING;

@Interceptor
public class ExceptionHandlingInterceptor {

	public static final String PROCESSING = Constants.OO_INFOSTATUS_PROCESSING;
	private static final org.slf4j.Logger ourLog =
			org.slf4j.LoggerFactory.getLogger(ExceptionHandlingInterceptor.class);
	public static final Set<SummaryEnum> SUMMARY_MODE = Collections.singleton(SummaryEnum.FALSE);
	private Class<?>[] myReturnStackTracesForExceptionTypes;

	@Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
	public boolean handleException(
			RequestDetails theRequestDetails,
			BaseServerResponseException theException,
			HttpServletRequest theRequest,
			HttpServletResponse theResponse)
			throws ServletException, IOException {
		handleException(theRequestDetails, theException);
		return false;
	}

	public Object handleException(RequestDetails theRequestDetails, BaseServerResponseException theException)
			throws ServletException, IOException {
		IRestfulResponse response = theRequestDetails.getResponse();

		FhirContext ctx = theRequestDetails.getServer().getFhirContext();

		IBaseOperationOutcome oo = theException.getOperationOutcome();
		if (oo == null) {
			oo = createOperationOutcome(theException, ctx);
		}

		int statusCode = theException.getStatusCode();

		// Add headers associated with the specific error code
		if (theException.hasResponseHeaders()) {
			Map<String, List<String>> additional = theException.getResponseHeaders();
			for (Entry<String, List<String>> next : additional.entrySet()) {
				if (isNotBlank(next.getKey()) && next.getValue() != null) {
					String nextKey = next.getKey();
					for (String nextValue : next.getValue()) {
						response.addHeader(nextKey, nextValue);
					}
				}
			}
		}

		String statusMessage = null;
		if (theException instanceof UnclassifiedServerFailureException) {
			String sm = theException.getMessage();
			if (isNotBlank(sm) && sm.indexOf('\n') == -1) {
				statusMessage = sm;
			}
		}

		BaseResourceReturningMethodBinding.callOutgoingFailureOperationOutcomeHook(theRequestDetails, oo);
		try {
			resetOutputStreamIfPossible(response);
		} catch (Throwable t) {
			ourLog.error(
					"HAPI-FHIR was unable to reset the output stream during exception handling. The root causes follows:",
					t);
		}

		return RestfulServerUtils.streamResponseAsResource(
				theRequestDetails.getServer(),
				oo,
				SUMMARY_MODE,
				statusCode,
				false,
				false,
				theRequestDetails,
				null,
				null);
	}

	/**
	 * In some edge cases, the output stream is opened already by the point at which an exception is thrown.
	 * This is a failsafe to that the output stream is writeable in that case. This operation retains status code and headers, but clears the buffer.
	 * Also, it strips the content-encoding header if present, as the method outcome will negotiate its own.
	 */
	private void resetOutputStreamIfPossible(IRestfulResponse response) {
		if (response.getClass().isAssignableFrom(ServletRestfulResponse.class)) {
			ServletRestfulResponse servletRestfulResponse = (ServletRestfulResponse) response;
			HttpServletResponse servletResponse =
					servletRestfulResponse.getRequestDetails().getServletResponse();
			Collection<String> headerNames = servletResponse.getHeaderNames();
			Map<String, Collection<String>> oldHeaders = new HashedMap<>();
			for (String headerName : headerNames) {
				oldHeaders.put(headerName, servletResponse.getHeaders(headerName));
			}

			servletResponse.reset();
			oldHeaders.entrySet().stream()
					.filter(entry -> !entry.getKey().equals(CONTENT_ENCODING))
					.forEach(entry -> {
						entry.getValue().stream().forEach(value -> {
							servletResponse.addHeader(entry.getKey(), value);
						});
					});
		}
	}

	@Hook(Pointcut.SERVER_PRE_PROCESS_OUTGOING_EXCEPTION)
	public BaseServerResponseException preProcessOutgoingException(
			RequestDetails theRequestDetails, Throwable theException, HttpServletRequest theServletRequest)
			throws ServletException {
		BaseServerResponseException retVal;
		if (theException instanceof DataFormatException) {
			// Wrapping the DataFormatException as an InvalidRequestException so that it gets sent back to the client as
			// a 400 response.
			retVal = new InvalidRequestException(theException);
		} else if (!(theException instanceof BaseServerResponseException)) {
			retVal = new InternalErrorException(theException);
		} else {
			retVal = (BaseServerResponseException) theException;
		}

		if (retVal.getOperationOutcome() == null) {
			retVal.setOperationOutcome(createOperationOutcome(
					theException, theRequestDetails.getServer().getFhirContext()));
		}

		return retVal;
	}

	private IBaseOperationOutcome createOperationOutcome(Throwable theException, FhirContext ctx)
			throws ServletException {
		IBaseOperationOutcome oo = null;
		if (theException instanceof BaseServerResponseException) {
			oo = ((BaseServerResponseException) theException).getOperationOutcome();
		}

		/*
		 * Generate an OperationOutcome to return, unless the exception throw by the resource provider had one
		 */
		if (oo == null) {
			try {
				oo = OperationOutcomeUtil.newInstance(ctx);

				if (theException instanceof InternalErrorException) {
					ourLog.error("Failure during REST processing", theException);
					populateDetails(ctx, theException, oo);
				} else if (theException instanceof BaseServerResponseException) {
					int statusCode = ((BaseServerResponseException) theException).getStatusCode();

					// No stack traces for non-server internal errors
					if (statusCode < 500) {
						ourLog.warn("Failure during REST processing: {}", theException.toString());
					} else {
						ourLog.warn("Failure during REST processing", theException);
					}

					BaseServerResponseException baseServerResponseException =
							(BaseServerResponseException) theException;
					populateDetails(ctx, theException, oo);
					if (baseServerResponseException.getAdditionalMessages() != null) {
						for (String next : baseServerResponseException.getAdditionalMessages()) {
							OperationOutcomeUtil.addIssue(ctx, oo, "error", next, null, PROCESSING);
						}
					}
				} else {
					ourLog.error("Failure during REST processing: " + theException.toString(), theException);
					populateDetails(ctx, theException, oo);
				}
			} catch (Exception e1) {
				ourLog.error("Failed to instantiate OperationOutcome resource instance", e1);
				throw new ServletException(
						Msg.code(328) + "Failed to instantiate OperationOutcome resource instance", e1);
			}
		} else {
			ourLog.error("Unknown error during processing", theException);
		}
		return oo;
	}

	private void populateDetails(FhirContext theCtx, Throwable theException, IBaseOperationOutcome theOo) {
		if (myReturnStackTracesForExceptionTypes != null) {
			for (Class<?> next : myReturnStackTracesForExceptionTypes) {
				if (next.isAssignableFrom(theException.getClass())) {
					String detailsValue =
							theException.getMessage() + "\n\n" + ExceptionUtils.getStackTrace(theException);
					OperationOutcomeUtil.addIssue(theCtx, theOo, "error", detailsValue, null, PROCESSING);
					return;
				}
			}
		}

		OperationOutcomeUtil.addIssue(theCtx, theOo, "error", theException.getMessage(), null, PROCESSING);
	}

	/**
	 * If any server methods throw an exception which extends any of the given exception types, the exception stack trace will be returned to the user. This can be useful for helping to diagnose
	 * issues, but may not be desirable for production situations.
	 *
	 * @param theExceptionTypes
	 *           The exception types for which to return the stack trace to the user.
	 * @return Returns an instance of this interceptor, to allow for easy method chaining.
	 */
	public ExceptionHandlingInterceptor setReturnStackTracesForExceptionTypes(Class<?>... theExceptionTypes) {
		myReturnStackTracesForExceptionTypes = theExceptionTypes;
		return this;
	}
}
