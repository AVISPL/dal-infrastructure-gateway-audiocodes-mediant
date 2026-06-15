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
		this.communicator.setHost("");
		this.communicator.setPort(8083);
		this.communicator.setLogin("");
		this.communicator.setPassword("");
		this.communicator.init();
	}

	@AfterEach
	void destroy() throws Exception {
		this.communicator.disconnect();
		this.communicator.destroy();
	}

	@Test
	void testLogin_withInvalidCredential() {
		Assertions.assertThrows(FailedLoginException.class, this.communicator::getMultipleStatistics);
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

	private Map<String, String> filterGroupStatistics(Map<String, String> statistics, String groupName) {
		return statistics.entrySet().stream()
				.filter(e -> groupName == null ? !e.getKey().contains(Constant.HASH) : e.getKey().startsWith(groupName))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private boolean isValidValue(String value) {
		return value != null && !value.isEmpty() && !value.isBlank();
	}
}
