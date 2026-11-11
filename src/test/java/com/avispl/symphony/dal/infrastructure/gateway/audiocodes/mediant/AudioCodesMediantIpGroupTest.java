/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.IpGroupStatsProperty;

/**
 * Integration tests for the per-IP-Group statistics, run against the AudioCodes Mediant simulator
 * (SBC variant) on {@code localhost:8083}. The simulator's IP Groups table is
 * 1 = PSTN1, 2 = PSTN2, 3 = Teams, 4 = ContactCenter.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
class AudioCodesMediantIpGroupTest {
	private static final Set<String> SIMULATOR_IP_GROUPS = Set.of(
			"IPGroupStatistics_1_PSTN1", "IPGroupStatistics_2_PSTN2", "IPGroupStatistics_3_Teams", "IPGroupStatistics_4_ContactCenter");

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
	}

	@Test
	void testGetMultipleStatistics_reportsEveryDiscoveredIpGroup() throws Exception {
		Assertions.assertEquals(SIMULATOR_IP_GROUPS, this.ipGroupNames(this.pollStatistics()));
	}

	@Test
	void testGetMultipleStatistics_reportsEveryPropertyForEachIpGroup() throws Exception {
		Map<String, String> stats = this.pollStatistics();

		for (String ipGroup : SIMULATOR_IP_GROUPS) {
			for (IpGroupStatsProperty property : IpGroupStatsProperty.values()) {
				String key = ipGroup + Constant.HASH + property.getName();
				Assertions.assertTrue(stats.containsKey(key), "Expected property " + key);
				Assertions.assertNotEquals(Constant.NOT_AVAILABLE, stats.get(key), "Expected a device value for " + key);
			}
		}
	}

	@Test
	void testGetMultipleStatistics_ipGroupValuesMatchSimulator() throws Exception {
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertEquals("42", stats.get("IPGroupStatistics_1_PSTN1#ActiveCallsIn"));
		Assertions.assertEquals("41", stats.get("IPGroupStatistics_2_PSTN2#MediaMOSIn"));
		Assertions.assertEquals("94", stats.get("IPGroupStatistics_3_Teams#AnswerSeizureRatio(%)"));
		Assertions.assertEquals("24", stats.get("IPGroupStatistics_4_ContactCenter#SIPRecSessions"));
		Assertions.assertEquals("120", stats.get("IPGroupStatistics_4_ContactCenter#RegisteredUsers"));
	}

	@Test
	void testGetMultipleStatistics_ipGroupStatisticsGroupNotDisplayed() throws Exception {
		this.communicator.setDisplayPropertyGroups(Constant.CALL_QUALITY_STATISTICS_GROUP);
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertTrue(stats.keySet().stream().noneMatch(key -> key.startsWith(Constant.IP_GROUP_STATISTICS_GROUP + Constant.UNDERSCORE)),
				"Expected no IP Group properties when " + Constant.IP_GROUP_STATISTICS_GROUP + " is not displayed");
		Assertions.assertTrue(stats.containsKey(Constant.CALL_QUALITY_STATISTICS_GROUP + Constant.HASH + "AnswerSeizureRatio(%)"));
	}

	@Test
	void testGetMultipleStatistics_ipGroupPropertiesAreNotHistorical() throws Exception {
		this.communicator.setHistoricalProperties("AnswerSeizureRatio(%)");
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		Map<String, String> dynamicStats = statistics.getDynamicStatistics();

		Assertions.assertTrue(dynamicStats.containsKey(Constant.CALL_QUALITY_STATISTICS_GROUP + Constant.HASH + "AnswerSeizureRatio(%)"));
		Assertions.assertTrue(dynamicStats.keySet().stream().noneMatch(key -> key.startsWith(Constant.IP_GROUP_STATISTICS_GROUP + Constant.UNDERSCORE)));
		Assertions.assertEquals("94", statistics.getStatistics().get("IPGroupStatistics_3_Teams#AnswerSeizureRatio(%)"));
	}

	@Test
	void testGetMultipleStatistics_ipGroupStatisticsServedFromCacheWithinRequestInterval() throws Exception {
		this.communicator.setIpGroupStatisticsRequestInterval("3600");
		String retrievedTimeKey = Constant.IP_GROUP_STATISTICS_GROUP + Constant.HASH + Constant.DATA_RETRIEVED_TIME;

		Map<String, String> firstPoll = this.pollStatistics();
		Assertions.assertEquals("42", firstPoll.get("IPGroupStatistics_1_PSTN1#ActiveCallsIn"));
		Assertions.assertTrue(firstPoll.containsKey(retrievedTimeKey));
		try {
			this.sendToSimulator("PUT", "/ipGroups/1", "{\"kpis\":{\"sbc/callStats\":{\"activeCallsIn\":7}}}");
			Map<String, String> secondPoll = this.pollStatistics();

			Assertions.assertEquals(SIMULATOR_IP_GROUPS, this.ipGroupNames(secondPoll));
			Assertions.assertEquals("42", secondPoll.get("IPGroupStatistics_1_PSTN1#ActiveCallsIn"));
			Assertions.assertEquals(firstPoll.get(retrievedTimeKey), secondPoll.get(retrievedTimeKey));
		} finally {
			this.sendToSimulator("POST", "/reset", "{}");
		}
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

	private Set<String> ipGroupNames(Map<String, String> stats) {
		return stats.keySet().stream()
				.filter(key -> key.startsWith(Constant.IP_GROUP_STATISTICS_GROUP + Constant.UNDERSCORE))
				.map(key -> key.substring(0, key.indexOf(Constant.HASH)))
				.collect(Collectors.toSet());
	}

	private Map<String, String> pollStatistics() throws Exception {
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);
		return statistics.getStatistics();
	}
}
