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

/**
 * Integration tests for the {@code MemoryStatistics}, {@code CPUStatistics} and {@code StorageStatistics} groups, run
 * against the AudioCodes Mediant simulator (SBC variant) on {@code localhost:8083}. The simulator's seed values
 * (real-time / last-interval average / last-interval maximum) are memory 48/45/71%, CPU 0 ("CPU 0") 23/20/64%,
 * CPU 1 41/38/88%, partition 0 ("Partition 0") 35/34/36% and 1450000/1440000/1460000 KB, and partition 1 12/12/13%
 * and 820000/815000/826000 KB.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
class AudioCodesMediantSystemTest {
	private static final String MEMORY = Constant.MEMORY_STATISTICS_GROUP + Constant.HASH;
	private static final String CPU_0 = Constant.CPU_STATISTICS_GROUP + "_0" + Constant.HASH;
	private static final String STORAGE_0 = Constant.STORAGE_STATISTICS_GROUP + "_0" + Constant.HASH;
	private static final String CPU_DATA_RETRIEVED_TIME = Constant.CPU_STATISTICS_GROUP + Constant.HASH + Constant.DATA_RETRIEVED_TIME;

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
	void testGetMultipleStatistics_reportsMemoryEveryCpuAndEveryPartition() throws Exception {
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertEquals(Set.of("MemoryUtilization(%)", "MemoryUtilizationAvg(%)", "MemoryUtilizationMax(%)"),
				this.propertiesOf(stats, Constant.MEMORY_STATISTICS_GROUP));
		Set<String> cpuProperties = Set.of(Constant.NAME, "CPUUtilization(%)", "CPUUtilizationAvg(%)", "CPUUtilizationMax(%)");
		Assertions.assertEquals(cpuProperties, this.propertiesOf(stats, Constant.CPU_STATISTICS_GROUP + "_0"));
		Assertions.assertEquals(cpuProperties, this.propertiesOf(stats, Constant.CPU_STATISTICS_GROUP + "_1"));
		Set<String> storageProperties = Set.of(Constant.NAME, "StorageUtilization(%)", "StorageUtilizationAvg(%)", "StorageUtilizationMax(%)",
				"StorageUsage(KB)", "StorageUsageAvg(KB)", "StorageUsageMax(KB)");
		Assertions.assertEquals(storageProperties, this.propertiesOf(stats, Constant.STORAGE_STATISTICS_GROUP + "_0"));
		Assertions.assertEquals(storageProperties, this.propertiesOf(stats, Constant.STORAGE_STATISTICS_GROUP + "_1"));
	}

	@Test
	void testGetMultipleStatistics_systemValuesMatchSimulator() throws Exception {
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertEquals("48", stats.get(MEMORY + "MemoryUtilization(%)"));
		Assertions.assertEquals("45", stats.get(MEMORY + "MemoryUtilizationAvg(%)"));
		Assertions.assertEquals("71", stats.get(MEMORY + "MemoryUtilizationMax(%)"));
		Assertions.assertEquals("CPU 0", stats.get(CPU_0 + Constant.NAME));
		Assertions.assertEquals("23", stats.get(CPU_0 + "CPUUtilization(%)"));
		Assertions.assertEquals("20", stats.get(CPU_0 + "CPUUtilizationAvg(%)"));
		Assertions.assertEquals("64", stats.get(CPU_0 + "CPUUtilizationMax(%)"));
		Assertions.assertEquals("88", stats.get(Constant.CPU_STATISTICS_GROUP + "_1" + Constant.HASH + "CPUUtilizationMax(%)"));
		Assertions.assertEquals("Partition 0", stats.get(STORAGE_0 + Constant.NAME));
		Assertions.assertEquals("35", stats.get(STORAGE_0 + "StorageUtilization(%)"));
		Assertions.assertEquals("1440000", stats.get(STORAGE_0 + "StorageUsageAvg(KB)"));
		Assertions.assertEquals("1460000", stats.get(STORAGE_0 + "StorageUsageMax(KB)"));
		Assertions.assertEquals("820000", stats.get(Constant.STORAGE_STATISTICS_GROUP + "_1" + Constant.HASH + "StorageUsage(KB)"));
	}

	@Test
	void testGetMultipleStatistics_onlyCpuStatisticsDisplayed() throws Exception {
		this.communicator.setDisplayPropertyGroups(Constant.CPU_STATISTICS_GROUP);
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertTrue(stats.containsKey(CPU_0 + "CPUUtilization(%)"));
		Assertions.assertTrue(stats.keySet().stream().noneMatch(key -> key.startsWith(MEMORY)
				|| key.startsWith(Constant.STORAGE_STATISTICS_GROUP)), "Expected only CPU Statistics of the system groups");
	}

	@Test
	void testGetMultipleStatistics_memoryUtilizationIsHistorical() throws Exception {
		this.communicator.setHistoricalProperties("MemoryUtilization(%)");
		var statistics = (ExtendedStatistics) this.communicator.getMultipleStatistics().get(0);

		Assertions.assertEquals(Map.of(MEMORY + "MemoryUtilization(%)", "48"), statistics.getDynamicStatistics());
		Assertions.assertEquals("48", statistics.getStatistics().get(MEMORY + "MemoryUtilization(%)"));
	}

	@Test
	void testGetMultipleStatistics_lastIntervalValuesRequestedEveryPollWithoutInterval() throws Exception {
		Assertions.assertEquals("64", this.pollStatistics().get(CPU_0 + "CPUUtilizationMax(%)"));
		this.sendToSimulator("PUT", "/state", "{\"cpu0UtilizationMax\":99}");
		Map<String, String> secondPoll = this.pollStatistics();

		Assertions.assertEquals("99", secondPoll.get(CPU_0 + "CPUUtilizationMax(%)"));
		Assertions.assertFalse(secondPoll.containsKey(CPU_DATA_RETRIEVED_TIME));
	}

	@Test
	void testGetMultipleStatistics_cpuValuesServedFromCacheWithinRequestInterval() throws Exception {
		this.communicator.setCpuStatisticsRequestInterval("3600");

		Map<String, String> firstPoll = this.pollStatistics();
		Assertions.assertEquals("23", firstPoll.get(CPU_0 + "CPUUtilization(%)"));
		Assertions.assertTrue(firstPoll.containsKey(CPU_DATA_RETRIEVED_TIME));

		this.sendToSimulator("PUT", "/state", "{\"cpu0Utilization\":95,\"memoryUtilization\":77}");
		Map<String, String> secondPoll = this.pollStatistics();

		Assertions.assertEquals("23", secondPoll.get(CPU_0 + "CPUUtilization(%)"));
		Assertions.assertEquals(firstPoll.get(CPU_DATA_RETRIEVED_TIME), secondPoll.get(CPU_DATA_RETRIEVED_TIME));
		Assertions.assertEquals("77", secondPoll.get(MEMORY + "MemoryUtilization(%)"));
	}

	private Set<String> propertiesOf(Map<String, String> stats, String group) {
		return stats.keySet().stream()
				.filter(key -> key.startsWith(group + Constant.HASH))
				.map(key -> key.substring(group.length() + Constant.HASH.length()))
				.collect(Collectors.toSet());
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
