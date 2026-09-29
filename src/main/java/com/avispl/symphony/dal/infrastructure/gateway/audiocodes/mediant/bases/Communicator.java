/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.security.auth.login.FailedLoginException;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.MethodNotSupportedException;

import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Logger;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * Configures the communicator and provides helper methods for managing adapter properties.
 * <p>This class centralizes all communicator-related configuration and exposes utility methods to access adapter properties.
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
public abstract class Communicator extends RestCommunicator {
	protected final Logger log = new Logger(super.logger);

	@Override
	protected void internalInit() throws Exception {
		this.setBaseUri("/api/v1");
		super.internalInit();
	}

	@Override
	protected void authenticate() throws Exception {
		if (StringUtils.isNullOrEmpty(super.getLogin(), true)
				|| StringUtils.isNullOrEmpty(super.getPassword(), true)) {
			throw new FailedLoginException("Failed to authenticate, the username or password has not provided");
		}
	}

	@Override
	protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) throws Exception {
		headers.setBasicAuth(super.getLogin(), super.getPassword(), StandardCharsets.UTF_8);
		return super.putExtraRequestHeaders(httpMethod, uri, headers);
	}

	/**
	 * Sends a GET request to the specified endpoint and deserializes the response
	 * into an instance of the given class.
	 *
	 * @param <T> the type of the response object
	 * @param clazz the class to deserialize the response into
	 * @param endpoint the target endpoint URL
	 * @return the deserialized response object
	 * @throws FailedLoginException if authentication fails
	 * @throws ResourceNotReachableException if the resource cannot be reached
	 * @throws Exception if the request fails for any other reason
	 */
	protected final <T> T fetchData(Class<T> clazz, String endpoint) throws Exception {
		return this.sendGetRequest(endpoint, clazz);
	}

	/**
	 * Sends a GET request to the specified endpoint with appended path segments,
	 * and deserializes the response into an instance of the given class.
	 *
	 * @param <T> the type of the response object
	 * @param clazz the class to deserialize the response into
	 * @param endpoint the base endpoint URL
	 * @param pathVariables one or more path segments to append to the endpoint
	 * @return the deserialized response object
	 * @throws FailedLoginException if authentication fails
	 * @throws ResourceNotReachableException if the resource cannot be reached
	 * @throws Exception if the request fails for any other reason
	 */
	protected final <T> T fetchData(Class<T> clazz, String endpoint, String... pathVariables) throws Exception {
		var uri = UriComponentsBuilder.fromUriString(endpoint).pathSegment(pathVariables).build().toUriString();
		return this.sendGetRequest(uri, clazz);
	}

	/**
	 * Sends a GET request to the specified endpoint with the provided query parameters,
	 * and deserializes the response into an instance of the given class.
	 *
	 * @param <T> the type of the response object
	 * @param clazz the class to deserialize the response into
	 * @param endpoint the base endpoint URL
	 * @param queryParams a map of query parameter names to their values
	 * @return the deserialized response object
	 * @throws FailedLoginException if authentication fails
	 * @throws ResourceNotReachableException if the resource cannot be reached
	 * @throws Exception if the request fails for any other reason
	 */
	protected final <T> T fetchData(Class<T> clazz, String endpoint, MultiValueMap<String, String> queryParams) throws Exception {
		var uri = UriComponentsBuilder.fromUriString(endpoint).queryParams(queryParams).build().toUriString();
		return this.sendGetRequest(uri, clazz);
	}

	/**
	 * Executes the underlying GET request and wraps unexpected exceptions with
	 * a descriptive {@link IllegalStateException}.
	 *
	 * @param <T> the type of the response object
	 * @param uri the fully-resolved URI to request
	 * @param responseClass the class to deserialize the response into
	 * @return the deserialized response object
	 * @throws FailedLoginException if authentication fails; propagated as-is
	 * @throws ResourceNotReachableException if the resource cannot be reached; propagated as-is
	 * @throws IllegalStateException if the request fails for any other reason
	 */
	private <T> T sendGetRequest(String uri, Class<T> responseClass) throws Exception {
		try {
			return super.doGet(uri, responseClass);
		} catch (FailedLoginException | ResourceNotReachableException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to send a request 'GET %s'".formatted(uri), e);
		}
	}

	/**
	 * Sends a POST request to the specified endpoint with the given request body.
	 *
	 * @param endpoint the target endpoint URL
	 * @param requestBody the object to serialize and send as the request body
	 * @throws FailedLoginException if authentication fails
	 * @throws ResourceNotReachableException if the resource cannot be reached
	 * @throws Exception if the request fails for any other reason
	 */
	protected final void performPost(String endpoint, Object requestBody) throws Exception {
		this.sendPerformRequest(Method.POST, endpoint, requestBody);
	}

	/**
	 * Sends a DELETE request to the specified endpoint.
	 *
	 * @param endpoint the target endpoint URL
	 * @throws FailedLoginException if authentication fails
	 * @throws ResourceNotReachableException if the resource cannot be reached
	 * @throws Exception if the request fails for any other reason
	 */
	protected final void performDelete(String endpoint) throws Exception {
		this.sendPerformRequest(Method.DELETE, endpoint, null);
	}

	/**
	 * Dispatches a POST or DELETE request to the specified URI and wraps unexpected
	 * exceptions with a descriptive {@link IllegalStateException}.
	 *
	 * @param method the HTTP method to use; must be {@link Method#POST} or {@link Method#DELETE}
	 * @param uri the fully-resolved URI to request
	 * @param requestBody the request body for POST requests; {@code null} for DELETE
	 * @throws MethodNotSupportedException if {@code method} is not POST or DELETE
	 * @throws FailedLoginException if authentication fails; propagated as-is
	 * @throws ResourceNotReachableException if the resource cannot be reached; propagated as-is
	 * @throws IllegalStateException if the request fails for any other reason
	 */
	private void sendPerformRequest(Method method, String uri, Object requestBody) throws Exception {
		try {
			switch (method) {
				case POST -> super.doPost(uri, requestBody);
				case DELETE -> super.doDelete(uri);
				default -> throw new MethodNotSupportedException("The adapter does not support HTTP method '%s'".formatted(method));
			}
		} catch (FailedLoginException | ResourceNotReachableException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to send a request '%s %s'".formatted(method, uri), e);
		}
	}
}
