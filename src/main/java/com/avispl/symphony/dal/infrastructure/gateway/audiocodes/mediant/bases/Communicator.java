/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases;

import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Logger;

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

	}
}
