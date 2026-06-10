/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.EndpointStatistics;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.Communicator;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Util;

/**
 * AudioCodesMediantCommunicator class
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
public class AudioCodesMediantCommunicator extends Communicator implements Monitorable, Controller {
	private ExtendedStatistics localExtendedStatistics = new ExtendedStatistics();
	private final EndpointStatistics localEndpointStatistics = new EndpointStatistics();
	private final ReentrantLock reentrantLock = new ReentrantLock();
	/** Adapter metadata properties - adapter version and build date */
	private Properties adapterProperties;

	/**
	 * Device adapter instantiation timestamp.
	 */
	private long adapterInitializationTimestamp;

	public AudioCodesMediantCommunicator() throws Exception {
		this.localExtendedStatistics.setStatistics(new HashMap<>());
		this.localExtendedStatistics.setControllableProperties(new ArrayList<>());
		adapterProperties = new Properties();
		adapterProperties.load(getClass().getResourceAsStream("/version.properties"));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}
		adapterInitializationTimestamp = System.currentTimeMillis();
		super.internalInit();
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
		reentrantLock.lock();
		try {
			ExtendedStatistics extStats = new ExtendedStatistics();
			Map<String, String> stats = new HashMap<>();
			retrieveMetadata(stats);

			extStats.setStatistics(stats);
			localExtendedStatistics = extStats;
		} finally {
			reentrantLock.unlock();
		}
		return List.of(this.localExtendedStatistics, this.localEndpointStatistics);
	}

	/**
	 * Retrieves metadata information and updates the provided statistics and dynamic map.
	 *
	 * @param stats the map where statistics will be stored
	 */
	private void retrieveMetadata(Map<String, String> stats) {
		try {
			stats.put( Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_VERSION,
					Util.getDefaultValueForNullData(adapterProperties.getProperty("adapter.version")));
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_BUILD_DATE,
					Util.getDefaultValueForNullData(adapterProperties.getProperty("adapter.build.date")));

			long adapterUptime = System.currentTimeMillis() - adapterInitializationTimestamp;
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_UPTIME, Util.formatUpTime(adapterUptime / 1000));
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_UPTIME_MIN, String.valueOf(adapterUptime / (1000 * 60)));
		} catch (Exception e) {
			logger.error("Failed to populate metadata information", e);
		}
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
