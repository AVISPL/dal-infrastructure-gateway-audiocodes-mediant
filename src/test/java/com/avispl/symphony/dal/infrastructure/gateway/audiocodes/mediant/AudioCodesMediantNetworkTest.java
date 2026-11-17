/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.EthernetPortStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.HaStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.SipTlsStatsProperty;

/**
 * Integration tests for the {@code HAStatistics}, {@code SIPTLSStatistics} and {@code EthernetPortStatistics} groups, run
 * against the AudioCodes Mediant simulator (SBC variant) on {@code localhost:8083}. The simulator's SIP TLS seed values
 * are 12 active connections (last-interval average 10, maximum 18), 4520 attempted connections (38 in the last interval)
 * and 27 rejected connections (1 in the last interval). The simulator's HA seed values (real-time / last-interval
 * average / last-interval maximum, in units of 0.01%) are 15/12/40 for the Active unit and 8/6/25 for the Redundant
 * unit. It seeds Ethernet ports 0 ("GE_1") and 1 ("GE_2"); each port's last-interval counters are 1/2000 of its
 * lifetime counters.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
class AudioCodesMediantNetworkTest {
	private static final String HA = Constant.HA_STATISTICS_GROUP + Constant.HASH;
	private static final String SIP_TLS = Constant.SIP_TLS_STATISTICS_GROUP + Constant.HASH;
	private static final String PORT_0 = Constant.ETHERNET_PORT_STATISTICS_GROUP + "_0" + Constant.HASH;
	private static final String PORT_DATA_RETRIEVED_TIME = Constant.ETHERNET_PORT_STATISTICS_GROUP + Constant.HASH + Constant.DATA_RETRIEVED_TIME;

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
	void testGetMultipleStatistics_reportsHaAndEveryEthernetPort() throws Exception {
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertEquals(Arrays.stream(HaStatsProperty.values()).map(HaStatsProperty::getName).collect(Collectors.toSet()),
				this.propertiesOf(stats, Constant.HA_STATISTICS_GROUP));
		Set<String> portProperties = Stream.concat(Stream.of(Constant.NAME),
				Arrays.stream(EthernetPortStatsProperty.values()).map(EthernetPortStatsProperty::getName)).collect(Collectors.toSet());
		Assertions.assertEquals(45, portProperties.size());
		Assertions.assertEquals(portProperties, this.propertiesOf(stats, Constant.ETHERNET_PORT_STATISTICS_GROUP + "_0"));
		Assertions.assertEquals(portProperties, this.propertiesOf(stats, Constant.ETHERNET_PORT_STATISTICS_GROUP + "_1"));
	}

	@Test
	void testGetMultipleStatistics_haValuesConvertedToPercent() throws Exception {
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertEquals("0.15", stats.get(HA + "HAActivePacketLoss(%)"));
		Assertions.assertEquals("0.12", stats.get(HA + "HAAverageActivePacketLoss(%)"));
		Assertions.assertEquals("0.40", stats.get(HA + "HAMaximumActivePacketLoss(%)"));
		Assertions.assertEquals("0.08", stats.get(HA + "HARedundantPacketLoss(%)"));
		Assertions.assertEquals("0.06", stats.get(HA + "HAAverageRedundantPacketLoss(%)"));
		Assertions.assertEquals("0.25", stats.get(HA + "HAMaximumRedundantPacketLoss(%)"));
	}

	@Test
	void testGetMultipleStatistics_sipTlsValuesMatchSimulator() throws Exception {
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertEquals(Arrays.stream(SipTlsStatsProperty.values()).map(SipTlsStatsProperty::getName).collect(Collectors.toSet()),
				this.propertiesOf(stats, Constant.SIP_TLS_STATISTICS_GROUP));
		Assertions.assertEquals("12", stats.get(SIP_TLS + "SIPTLSActiveConnections"));
		Assertions.assertEquals("10", stats.get(SIP_TLS + "SIPTLSAverageActiveConnections"));
		Assertions.assertEquals("18", stats.get(SIP_TLS + "SIPTLSMaximumActiveConnections"));
		Assertions.assertEquals("4520", stats.get(SIP_TLS + "SIPTLSAttemptedConnectionsTotal"));
		Assertions.assertEquals("38", stats.get(SIP_TLS + "SIPTLSAttemptedConnections"));
		Assertions.assertEquals("27", stats.get(SIP_TLS + "SIPTLSRejectedConnectionsTotal"));
		Assertions.assertEquals("1", stats.get(SIP_TLS + "SIPTLSRejectedConnections"));
	}

	@Test
	void testGetMultipleStatistics_sipTlsValuesServedFromCacheWithinRequestInterval() throws Exception {
		this.communicator.setSipTlsStatisticsRequestInterval("3600");
		String retrievedTimeKey = SIP_TLS + Constant.DATA_RETRIEVED_TIME;

		Map<String, String> firstPoll = this.pollStatistics();
		Assertions.assertTrue(firstPoll.containsKey(retrievedTimeKey));

		this.sendToSimulator("PUT", "/state", "{\"activeSipTlsConn\":99}");
		Map<String, String> secondPoll = this.pollStatistics();

		Assertions.assertEquals("12", secondPoll.get(SIP_TLS + "SIPTLSActiveConnections"));
		Assertions.assertEquals(firstPoll.get(retrievedTimeKey), secondPoll.get(retrievedTimeKey));
	}

	@Test
	void testGetMultipleStatistics_portValuesMatchSimulator() throws Exception {
		this.sendToSimulator("PUT", "/ports/0", "{\"kpis\":{\"bytesInTotal\":5000000,\"bytesIn\":2500,\"fcsErrorInTotal\":7,\"fcsErrorIn\":1}}");
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertEquals("GE_1", stats.get(PORT_0 + Constant.NAME));
		Assertions.assertEquals("5000000", stats.get(PORT_0 + "BytesInTotal"));
		Assertions.assertEquals("2500", stats.get(PORT_0 + "BytesIn"));
		Assertions.assertEquals("7", stats.get(PORT_0 + "FCSErrorInTotal"));
		Assertions.assertEquals("1", stats.get(PORT_0 + "FCSErrorIn"));
		Assertions.assertEquals("GE_2", stats.get(Constant.ETHERNET_PORT_STATISTICS_GROUP + "_1" + Constant.HASH + Constant.NAME));
	}

	@Test
	void testGetMultipleStatistics_addedAndRemovedPortsFollowDevice() throws Exception {
		this.sendToSimulator("POST", "/ports", "{\"id\":5,\"name\":\"GE_6\",\"description\":\"Uplink\"}");
		this.sendToSimulator("DELETE", "/ports/1", "");
		Map<String, String> stats = this.pollStatistics();

		String port5 = Constant.ETHERNET_PORT_STATISTICS_GROUP + "_5" + Constant.HASH;
		Assertions.assertEquals("GE_6", stats.get(port5 + Constant.NAME));
		Assertions.assertEquals("0", stats.get(port5 + "BytesInTotal"));
		Assertions.assertEquals("0", stats.get(port5 + "BytesIn"));
		Assertions.assertTrue(stats.keySet().stream().noneMatch(key -> key.startsWith(Constant.ETHERNET_PORT_STATISTICS_GROUP + "_1" + Constant.HASH)));
	}

	@Test
	void testGetMultipleStatistics_onlyHaStatisticsDisplayed() throws Exception {
		this.communicator.setDisplayPropertyGroups(Constant.HA_STATISTICS_GROUP);
		Map<String, String> stats = this.pollStatistics();

		Assertions.assertTrue(stats.containsKey(HA + "HAActivePacketLoss(%)"));
		Assertions.assertTrue(stats.keySet().stream().noneMatch(key -> key.startsWith(Constant.ETHERNET_PORT_STATISTICS_GROUP)),
				"Expected no Ethernet Port Statistics");
	}

	@Test
	void testGetMultipleStatistics_portValuesServedFromCacheWithinRequestInterval() throws Exception {
		this.communicator.setEthernetPortStatisticsRequestInterval("3600");

		Map<String, String> firstPoll = this.pollStatistics();
		Assertions.assertTrue(firstPoll.containsKey(PORT_DATA_RETRIEVED_TIME));
		String bytesIn = firstPoll.get(PORT_0 + "BytesInTotal");

		this.sendToSimulator("PUT", "/ports/0", "{\"kpis\":{\"bytesInTotal\":1}}");
		this.sendToSimulator("PUT", "/state", "{\"haActivePacketLoss\":250}");
		Map<String, String> secondPoll = this.pollStatistics();

		Assertions.assertEquals(bytesIn, secondPoll.get(PORT_0 + "BytesInTotal"));
		Assertions.assertEquals(firstPoll.get(PORT_DATA_RETRIEVED_TIME), secondPoll.get(PORT_DATA_RETRIEVED_TIME));
		Assertions.assertEquals("2.50", secondPoll.get(HA + "HAActivePacketLoss(%)"));
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
