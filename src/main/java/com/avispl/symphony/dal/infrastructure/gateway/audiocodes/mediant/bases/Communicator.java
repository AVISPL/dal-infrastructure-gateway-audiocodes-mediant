/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import javax.security.auth.login.FailedLoginException;

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
}
