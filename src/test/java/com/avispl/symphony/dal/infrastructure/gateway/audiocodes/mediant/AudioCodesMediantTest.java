/**
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.util.Map;
import java.util.stream.Collectors;

import javax.security.auth.login.FailedLoginException;
import org.apache.commons.collections.MapUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;

/**
 * AudioCodesMediantTest class
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
class AudioCodesMediantTest {
	private AudioCodesMediantCommunicator communicator;

	@BeforeEach
	void setUp() throws Exception {
		communicator = new AudioCodesMediantCommunicator();
		this.communicator.setHost("localhost");
		this.communicator.setPort(8083);
		this.communicator.setLogin("admin");
		this.communicator.setPassword("admin");
		this.communicator.init();
	}

	@AfterEach
	void destroy() throws Exception {
		this.communicator.disconnect();
		this.communicator.destroy();
	}

	@Test
	void testLogin_withInvalidCredential() throws Exception {
		var invalidCredsCommunicator = new AudioCodesMediantCommunicator();
		invalidCredsCommunicator.setHost("localhost");
		invalidCredsCommunicator.setPort(8083);
		invalidCredsCommunicator.setLogin("admin");
		invalidCredsCommunicator.setPassword("wrong-password");
		invalidCredsCommunicator.init();
		try {
			Assertions.assertThrows(FailedLoginException.class, invalidCredsCommunicator::getMultipleStatistics);
		} finally {
			invalidCredsCommunicator.disconnect();
			invalidCredsCommunicator.destroy();
		}
	}

	@Test
	void testGetMultipleStatistics_withGeneral() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		var generalProperties = this.filterGroupStatistics(statistics.getStatistics(), null);

		Assertions.assertTrue(MapUtils.isNotEmpty(generalProperties));
		generalProperties.forEach((pName, pValue) -> Assertions.assertTrue(this.isValidValue(pValue)));
	}

	@Test
	void testGetMultipleStatistics_withNetworkGroup() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		var networkGroup = this.filterGroupStatistics(statistics.getStatistics(), Constant.NETWORK_GROUP);

		Assertions.assertTrue(MapUtils.isNotEmpty(networkGroup));
		networkGroup.forEach((pName, pValue) -> Assertions.assertTrue(this.isValidValue(pValue)));
	}

	@Test
	void testGetMultipleStatistics_withCallStatsGroup() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		var callStatsGroup = this.filterGroupStatistics(statistics.getStatistics(), Constant.CALL_STATS_GROUP);

		Assertions.assertTrue(MapUtils.isNotEmpty(callStatsGroup));
		callStatsGroup.forEach((pName, pValue) -> Assertions.assertTrue(this.isValidValue(pValue)));
	}

	@Test
	void testControlProperty_startAndStopCallDiagnostic() throws Exception {
		String calledNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLED_NUMBER;
		String callingNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLING_NUMBER;
		String destinationKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_DESTINATION;
		String statusKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STATUS;
		String startKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_START;
		String stopKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STOP;

		this.communicator.getMultipleStatistics();

		this.communicator.controlProperty(new ControllableProperty(calledNumberKey, "200", null));
		this.communicator.controlProperty(new ControllableProperty(callingNumberKey, "100", null));
		this.communicator.controlProperty(new ControllableProperty(destinationKey, "10.4.219.229", null));
		this.communicator.controlProperty(new ControllableProperty(startKey, "1", null));

		var afterStart = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		String statusAfterStart = afterStart.getStatistics().get(statusKey);
		Assertions.assertTrue(this.isValidValue(statusAfterStart));
		Assertions.assertNotEquals(Constant.CALL_DIAGNOSTICS_NOT_DIALED, statusAfterStart);

		this.communicator.controlProperty(new ControllableProperty(stopKey, "1", null));

		var afterStop = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		Assertions.assertEquals(Constant.CALL_DIAGNOSTICS_DISCONNECTED, afterStop.getStatistics().get(statusKey));
	}

	private Map<String, String> filterGroupStatistics(Map<String, String> statistics, String groupName) {
		return statistics.entrySet().stream()
				.filter(e -> groupName == null ? !e.getKey().contains(Constant.HASH) : e.getKey().startsWith(groupName))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private boolean isValidValue(String value) {
		return value != null && !value.isEmpty() && !value.isBlank();
	}
}
