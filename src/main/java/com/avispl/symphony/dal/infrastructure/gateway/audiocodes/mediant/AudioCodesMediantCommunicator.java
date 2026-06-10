/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.EndpointStatistics;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.Communicator;

/**
 * AudioCodesMediantCommunicator class
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
public class AudioCodesMediantCommunicator extends Communicator implements Monitorable, Controller {
	private final ExtendedStatistics localExtendedStatistics = new ExtendedStatistics();
	private final EndpointStatistics localEndpointStatistics = new EndpointStatistics();

	public AudioCodesMediantCommunicator() {
		this.localExtendedStatistics.setStatistics(new HashMap<>());
		this.localExtendedStatistics.setControllableProperties(new ArrayList<>());
	}

	@Override
	protected void internalDestroy() {
		//  Clear the extended properties
		this.localExtendedStatistics.setStatistics(new HashMap<>());
		this.localExtendedStatistics.setControllableProperties(new ArrayList<>());
		super.internalDestroy();
	}

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		return List.of(this.localExtendedStatistics, this.localEndpointStatistics);
	}

	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		throw new UnsupportedOperationException("Currently, property control is not supported.");
	}

	@Override
	public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
		throw new UnsupportedOperationException("Currently, the feature to control properties is not supported.");
	}
}
