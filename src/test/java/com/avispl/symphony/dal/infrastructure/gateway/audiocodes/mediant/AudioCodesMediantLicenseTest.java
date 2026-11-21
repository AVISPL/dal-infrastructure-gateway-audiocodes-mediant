/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.LicenseStatsProperty;

/**
 * Integration tests for the {@code LicenseStatistics} group, run against the AudioCodes Mediant simulator
 * (SBC variant) on {@code localhost:8083}. The simulator's SBC seed values are SBC signaling 40%, SBC media 36%,
 * transcoding 20%, SIP-Rec 25%, WebRTC 0% and FEU 0%.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
class AudioCodesMediantLicenseTest {
	private static final String SIGNALING_USAGE = Constant.LICENSE_STATISTICS_GROUP + Constant.HASH + "LicenseSBCSignalingUsage(%)";
	private static final String DATA_RETRIEVED_TIME = Constant.LICENSE_STATISTICS_GROUP + Constant.HASH + Constant.DATA_RETRIEVED_TIME;

	private AudioCodesMediantCommunicator communicator;

	@BeforeEach
	void setUp() throws Exception {
		this.communicator = new AudioCodesMediantCommunicator();
		this.communicator.setHost("localhost");
		this.communicator.setPort(8083);
		this.communicator.setLogin("admin");
		this.communicator.setPassword("admin");
		this.communicator.setDisplayPropertyGroups(Constant.CALL_STATS_ALL_GROUPS);
		this.communicator.init();
	}

	@AfterEach
	void destroy() throws Exception {
		this.communicator.disconnect();
		this.communicator.destroy();
		this.sendToSimulator("POST", "/reset", "{}");
	}

	@Test
	void testGetMultipleStatistics_reportsEveryLicenseProperty() throws Exception {
		Map<String, String> stats = this.pollStatistics();

		for (LicenseStatsProperty property : LicenseStatsProperty.values()) {
			String key = Constant.LICENSE_STATISTICS_GROUP + Constant.HASH + property.getName();
			Assertions.assertTrue(stats.containsKey(key), "Expected property " + key);
			Assertions.assertNotEquals(Constant.NOT_AVAILABLE, stats.get(key), "Expected a device value for " + key);
		}
	}

	@Test
	void testGetMultipleStatistics_licenseValuesMatchSimulator() throws Exception {
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertEquals("40", stats.get(SIGNALING_USAGE));
		Assertions.assertEquals("36", stats.get(Constant.LICENSE_STATISTICS_GROUP + Constant.HASH + "LicenseSBCMediaUsage(%)"));
		Assertions.assertEquals("20", stats.get(Constant.LICENSE_STATISTICS_GROUP + Constant.HASH + "LicenseTranscodingUsage(%)"));
		Assertions.assertEquals("25", stats.get(Constant.LICENSE_STATISTICS_GROUP + Constant.HASH + "LicenseSIPRecUsage(%)"));
		Assertions.assertEquals("0", stats.get(Constant.LICENSE_STATISTICS_GROUP + Constant.HASH + "LicenseWebRTCUsage(%)"));
		Assertions.assertEquals("0", stats.get(Constant.LICENSE_STATISTICS_GROUP + Constant.HASH + "LicenseFEUUsage(%)"));
	}

	@Test
	void testGetMultipleStatistics_licenseStatisticsGroupNotDisplayed() throws Exception {
		this.communicator.setDisplayPropertyGroups(Constant.CALL_QUALITY_STATISTICS_GROUP);
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertTrue(stats.keySet().stream().noneMatch(key -> key.startsWith(Constant.LICENSE_STATISTICS_GROUP + Constant.HASH)),
				"Expected no License Statistics properties when " + Constant.LICENSE_STATISTICS_GROUP + " is not displayed");
	}

	@Test
	void testGetMultipleStatistics_licenseValuesRequestedEveryPollWithoutInterval() throws Exception {
		Assertions.assertEquals("40", this.pollStatistics().get(SIGNALING_USAGE));
		this.sendToSimulator("PUT", "/state", "{\"licenseSbcSignalingUsage\":77}");
		Map<String, String> secondPoll = this.pollStatistics();

		Assertions.assertEquals("77", secondPoll.get(SIGNALING_USAGE));
		Assertions.assertFalse(secondPoll.containsKey(DATA_RETRIEVED_TIME));
	}

	@Test
	void testGetMultipleStatistics_licenseValuesServedFromCacheWithinRequestInterval() throws Exception {
		this.communicator.setLicenseStatisticsRequestInterval("3600");

		Map<String, String> firstPoll = this.pollStatistics();
		Assertions.assertEquals("40", firstPoll.get(SIGNALING_USAGE));
		Assertions.assertTrue(firstPoll.containsKey(DATA_RETRIEVED_TIME));

		this.sendToSimulator("PUT", "/state", "{\"licenseSbcSignalingUsage\":77}");
		Map<String, String> secondPoll = this.pollStatistics();

		Assertions.assertEquals("40", secondPoll.get(SIGNALING_USAGE));
		Assertions.assertEquals(firstPoll.get(DATA_RETRIEVED_TIME), secondPoll.get(DATA_RETRIEVED_TIME));
	}

	/**
	 * Sends a request to the simulator's control API ({@code /api/simulator}).
	 */
	private void sendToSimulator(String method, String path, String body) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8083/api/simulator" + path))
				.header("Content-Type", "application/json")
				.method(method, HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, response.statusCode(), "Simulator control request failed: " + response.body());
	}

	private Map<String, String> pollStatistics() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		return statistics.getStatistics();
	}
}
