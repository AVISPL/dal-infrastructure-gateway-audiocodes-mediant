/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;

/**
 * Tests the per-group request intervals ({@code <group>RequestInterval}) of {@link AudioCodesMediantCommunicator}
 * without a device: {@code doGet} is overridden to answer from an in-memory table and count requests per URI, and the
 * adapter's clock is replaced so a test decides how much time passes between cycles.
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
@Tag("unit")
class AudioCodesMediantRequestIntervalTest {
	/** 2026-11-06T15:04:05Z: an afternoon hour, so a 12-hour format or a non-UTC zone would show in the timestamp. */
	private static final long START = Instant.parse("2026-11-06T15:04:05Z").toEpochMilli();
	private static final long SECOND = 1000L;

	private static final String CALL_LOAD_KPI = Constant.CALL_STATS_KPI_API + "/activeSessions";
	private static final String MEDIA_KPI = Constant.MEDIA_STATS_KPI_API + "/mediaJitterIn";
	private static final String ACTIVE_SESSIONS_KEY = Constant.CALL_LOAD_STATISTICS_GROUP + Constant.HASH + "ActiveSessions";
	private static final String CALL_LOAD_RETRIEVED_KEY = Constant.CALL_LOAD_STATISTICS_GROUP + Constant.HASH + Constant.DATA_RETRIEVED_TIME;
	private static final String ALARMS_RETRIEVED_KEY = Constant.ACTIVE_ALARM + Constant.HASH + Constant.DATA_RETRIEVED_TIME;

	/**
	 * Device stand-in: the adapter's HTTP requests go through a {@link RestTemplate} that answers from
	 * {@link #responses} (by path, without the {@code /api/v1} base), returns HTTP 500 for paths in
	 * {@link #failingUris}, and counts every request by path. Paths not listed answer {@code {}}.
	 */
	private static final class FakeDeviceCommunicator extends AudioCodesMediantCommunicator {
		final Map<String, String> responses = new ConcurrentHashMap<>();
		final Set<String> failingUris = ConcurrentHashMap.newKeySet();
		final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

		FakeDeviceCommunicator() throws Exception {
			super();
		}

		@Override
		protected RestTemplate obtainRestTemplate() {
			RestTemplate template = new RestTemplate();
			template.setRequestFactory((uri, method) -> {
				String path = uri.getPath().replaceFirst("^/api/v1", "").replaceFirst("^//", "/");
				requestCounts.computeIfAbsent(path, key -> new AtomicInteger()).incrementAndGet();
				MockClientHttpRequest request = new MockClientHttpRequest(method, uri);
				MockClientHttpResponse response = failingUris.contains(path)
						? new MockClientHttpResponse("{}".getBytes(StandardCharsets.UTF_8), HttpStatus.INTERNAL_SERVER_ERROR)
						: new MockClientHttpResponse(responses.getOrDefault(path, "{}").getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
				response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
				request.setResponse(response);
				return request;
			});
			return template;
		}

		int requests(String uri) {
			AtomicInteger count = requestCounts.get(uri);
			return count == null ? 0 : count.get();
		}
	}

	private FakeDeviceCommunicator communicator;
	private final AtomicLong now = new AtomicLong(START);

	@BeforeEach
	void setUp() throws Exception {
		communicator = new FakeDeviceCommunicator();
		communicator.setHost("127.0.0.1");
		communicator.setLogin("user");
		communicator.setPassword("password");
		communicator.setClock(now::get);
		communicator.responses.put(CALL_LOAD_KPI, "{\"id\":\"activeSessions\",\"value\":\"5\"}");
		communicator.responses.put(Constant.ACTIVE_ALARMS_API, "{\"alarms\":[{\"id\":7}]}");
		communicator.responses.put(Constant.ACTIVE_ALARMS_API + "/7", "{\"id\":7,\"description\":\"Fan\",\"severity\":\"major\",\"source\":\"Board#1\"}");
	}

	@AfterEach
	void tearDown() {
		communicator.destroy();
	}

	private Map<String, String> poll() throws Exception {
		return ((ExtendedStatistics) communicator.getMultipleStatistics().get(0)).getStatistics();
	}

	private Map<String, String> pollDynamic() throws Exception {
		return ((ExtendedStatistics) communicator.getMultipleStatistics().get(0)).getDynamicStatistics();
	}

	private void advanceSeconds(long seconds) {
		now.addAndGet(seconds * SECOND);
	}

	/**
	 * An interval that has never been set reads as the default of 60 seconds.
	 */
	@Test
	void testDefaultIsSixtySeconds() {
		Assertions.assertEquals("60", communicator.getCallLoadStatisticsRequestInterval());
		Assertions.assertEquals("60", communicator.getActiveAlarmsRequestInterval());
	}

	/**
	 * Whole numbers from 60 to 86400 are kept, with surrounding whitespace ignored.
	 */
	@Test
	void testValidValuesAreKept() {
		communicator.setMediaStatisticsRequestInterval("60");
		Assertions.assertEquals("60", communicator.getMediaStatisticsRequestInterval());
		communicator.setMediaStatisticsRequestInterval(" 3600 ");
		Assertions.assertEquals("3600", communicator.getMediaStatisticsRequestInterval());
		communicator.setMediaStatisticsRequestInterval("86400");
		Assertions.assertEquals("86400", communicator.getMediaStatisticsRequestInterval());
	}

	/**
	 * Below-minimum, above-maximum, negative, fractional and non-numeric values all fall back to 60; an above-maximum
	 * value is not capped at 86400.
	 */
	@Test
	void testInvalidValuesFallBackToDefault() {
		for (String invalid : List.of("59", "86401", "-120", "1.5", "abc", "120s")) {
			communicator.setSipRecStatisticsRequestInterval("3600");
			communicator.setSipRecStatisticsRequestInterval(invalid);
			Assertions.assertEquals("60", communicator.getSipRecStatisticsRequestInterval(), "value '" + invalid + "'");
		}
	}

	/**
	 * A group with no interval set is requested at every cycle and gets no DataRetrievedTime, as before request
	 * intervals existed.
	 */
	@Test
	void testGroupWithoutIntervalIsRequestedEveryCycle() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		communicator.init();

		Map<String, String> first = poll();
		poll();

		Assertions.assertEquals(2, communicator.requests(CALL_LOAD_KPI));
		Assertions.assertEquals("5", first.get(ACTIVE_SESSIONS_KEY));
		Assertions.assertFalse(first.containsKey(CALL_LOAD_RETRIEVED_KEY));
	}

	/**
	 * A group with an interval set is served from its cached result until the interval passes, then requested again.
	 */
	@Test
	void testConfiguredGroupIsReusedUntilIntervalPasses() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		communicator.setCallLoadStatisticsRequestInterval("300");
		communicator.init();

		poll();
		communicator.responses.put(CALL_LOAD_KPI, "{\"value\":\"9\"}");
		advanceSeconds(60);
		Map<String, String> cached = poll();
		advanceSeconds(240);
		Map<String, String> refreshed = poll();

		Assertions.assertEquals("5", cached.get(ACTIVE_SESSIONS_KEY), "value inside the interval comes from the cache");
		Assertions.assertEquals("9", refreshed.get(ACTIVE_SESSIONS_KEY), "value after the interval is requested again");
		Assertions.assertEquals(2, communicator.requests(CALL_LOAD_KPI));
	}

	/**
	 * A cycle that arrives up to the tolerance early still requests the group; one that arrives earlier does not.
	 */
	@Test
	void testCycleArrivingSlightlyEarlyStillRequestsGroup() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		communicator.setCallLoadStatisticsRequestInterval("120");
		communicator.init();

		poll();
		advanceSeconds(114);
		poll();
		Assertions.assertEquals(1, communicator.requests(CALL_LOAD_KPI), "6 s early is outside the tolerance");

		advanceSeconds(1);
		poll();
		Assertions.assertEquals(2, communicator.requests(CALL_LOAD_KPI), "5 s early is within the tolerance");
	}

	/**
	 * The default interval of 60 seconds set explicitly still refreshes at every 60-second cycle, even one that
	 * arrives slightly early.
	 */
	@Test
	void testSixtySecondIntervalRefreshesEveryCycle() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		communicator.setCallLoadStatisticsRequestInterval("60");
		communicator.init();

		poll();
		now.addAndGet(59_900L);
		poll();
		now.addAndGet(60_000L);
		poll();

		Assertions.assertEquals(3, communicator.requests(CALL_LOAD_KPI));
	}

	/**
	 * A group with a failed request is requested again at the next cycle, whatever its interval.
	 */
	@Test
	void testFailedGroupIsRequestedAgainNextCycle() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		communicator.setCallLoadStatisticsRequestInterval("3600");
		communicator.init();
		communicator.failingUris.add(CALL_LOAD_KPI);

		Map<String, String> failed = poll();
		communicator.failingUris.clear();
		advanceSeconds(60);
		Map<String, String> retried = poll();
		advanceSeconds(60);
		poll();

		Assertions.assertEquals(Constant.NOT_AVAILABLE, failed.get(ACTIVE_SESSIONS_KEY));
		Assertions.assertEquals("5", retried.get(ACTIVE_SESSIONS_KEY));
		Assertions.assertEquals(2, communicator.requests(CALL_LOAD_KPI), "requested on failure and on the retry, then cached");
	}

	/**
	 * DataRetrievedTime is shown only for groups with an interval set, in 24-hour UTC, and keeps the time of the last
	 * request while the group is served from the cache.
	 */
	@Test
	void testDataRetrievedTimeIsUtcAndOnlyForConfiguredGroups() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP + "," + Constant.MEDIA_STATISTICS_GROUP);
		communicator.setCallLoadStatisticsRequestInterval("300");
		communicator.init();

		poll();
		advanceSeconds(60);
		Map<String, String> stats = poll();

		Assertions.assertEquals("2026-11-06T15:04:05Z", stats.get(CALL_LOAD_RETRIEVED_KEY));
		Assertions.assertFalse(stats.containsKey(Constant.MEDIA_STATISTICS_GROUP + Constant.HASH + Constant.DATA_RETRIEVED_TIME));
	}

	/**
	 * Clearing an interval (blank value) makes the group uncached again: it is requested every cycle and loses its
	 * DataRetrievedTime.
	 */
	@Test
	void testBlankValueRemovesInterval() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		communicator.setCallLoadStatisticsRequestInterval("3600");
		communicator.init();
		poll();

		communicator.setCallLoadStatisticsRequestInterval("");
		Map<String, String> stats = poll();

		Assertions.assertEquals(2, communicator.requests(CALL_LOAD_KPI));
		Assertions.assertFalse(stats.containsKey(CALL_LOAD_RETRIEVED_KEY));
	}

	/**
	 * An interval set for a group that displayPropertyGroups does not enable has no effect: the group is not requested
	 * and shows no properties.
	 */
	@Test
	void testIntervalOfDisabledGroupIsIgnored() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		communicator.setMediaStatisticsRequestInterval("300");
		communicator.init();

		Map<String, String> stats = poll();

		Assertions.assertEquals(0, communicator.requests(MEDIA_KPI));
		Assertions.assertTrue(stats.keySet().stream().noneMatch(key -> key.startsWith(Constant.MEDIA_STATISTICS_GROUP + Constant.HASH)));
	}

	/**
	 * A historical property keeps being reported as a dynamic statistic from the cached value while its group is
	 * inside the interval, rather than leaving a gap.
	 */
	@Test
	void testHistoricalPropertyIsReportedFromCachedValue() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		communicator.setHistoricalProperties("ActiveSessions");
		communicator.setCallLoadStatisticsRequestInterval("300");
		communicator.init();

		pollDynamic();
		advanceSeconds(60);
		Map<String, String> dynamic = pollDynamic();

		Assertions.assertEquals(1, communicator.requests(CALL_LOAD_KPI));
		Assertions.assertEquals("5", dynamic.get(ACTIVE_SESSIONS_KEY));
	}

	/**
	 * ActiveAlarms with an interval set reuses the alarm list and the per-alarm details inside the interval.
	 */
	@Test
	void testActiveAlarmsAreCached() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.ACTIVE_ALARM);
		communicator.setActiveAlarmsRequestInterval("300");
		communicator.init();

		poll();
		advanceSeconds(60);
		Map<String, String> stats = poll();

		Assertions.assertEquals(1, communicator.requests(Constant.ACTIVE_ALARMS_API));
		Assertions.assertEquals(1, communicator.requests(Constant.ACTIVE_ALARMS_API + "/7"));
		Assertions.assertEquals("1", stats.get(Constant.ACTIVE_ALARM + Constant.HASH + Constant.COUNT));
		Assertions.assertEquals("2026-11-06T15:04:05Z", stats.get(ALARMS_RETRIEVED_KEY));
	}

	/**
	 * General and Network come from /status, which is requested at every cycle even when every cacheable group has an
	 * interval set.
	 */
	@Test
	void testStatusIsRequestedEveryCycle() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_STATS_ALL_GROUPS);
		communicator.setCallLoadStatisticsRequestInterval("3600");
		communicator.setActiveAlarmsRequestInterval("3600");
		communicator.init();

		poll();
		advanceSeconds(60);
		Map<String, String> stats = poll();

		Assertions.assertEquals(2, communicator.requests(Constant.STATUS_ENDPOINT));
		Assertions.assertTrue(stats.containsKey(Constant.NETWORK_GROUP + Constant.HASH + "IPAddress"), "Network is shown");
		Assertions.assertFalse(stats.containsKey(Constant.NETWORK_GROUP + Constant.HASH + Constant.DATA_RETRIEVED_TIME),
				"Network has no interval and no DataRetrievedTime");
		Assertions.assertFalse(stats.containsKey(Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.DATA_RETRIEVED_TIME));
	}

	/**
	 * CallDiagnostics is never cached: a value staged by a control is shown at the next cycle instead of being
	 * replaced by an earlier result.
	 */
	@Test
	void testCallDiagnosticsControlIsShownAtNextCycle() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_DIAGNOSTICS_GROUP);
		communicator.init();
		String calledNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLED_NUMBER;
		poll();

		ControllableProperty control = new ControllableProperty();
		control.setProperty(calledNumberKey);
		control.setValue("1234");
		communicator.controlProperty(control);
		advanceSeconds(60);
		Map<String, String> stats = poll();

		Assertions.assertEquals("1234", stats.get(calledNumberKey));
	}

	/**
	 * A cached group marked for refresh, as a control of that group does, is requested at the next cycle even inside
	 * its interval, and is cached again afterwards.
	 */
	@Test
	void testGroupMarkedForRefreshIsRequestedNextCycle() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		communicator.setCallLoadStatisticsRequestInterval("3600");
		communicator.init();

		poll();
		communicator.refreshGroupOnNextCycle(Constant.CALL_LOAD_STATISTICS_GROUP);
		advanceSeconds(60);
		Map<String, String> stats = poll();
		advanceSeconds(60);
		poll();

		Assertions.assertEquals(2, communicator.requests(CALL_LOAD_KPI));
		Assertions.assertEquals("2026-11-06T15:05:05Z", stats.get(CALL_LOAD_RETRIEVED_KEY));
	}

	/**
	 * destroy() drops the cache, so a re-initialised adapter requests every group at its first cycle.
	 */
	@Test
	void testDestroyClearsCache() throws Exception {
		communicator.setDisplayPropertyGroups(Constant.CALL_LOAD_STATISTICS_GROUP);
		communicator.setCallLoadStatisticsRequestInterval("3600");
		communicator.init();
		poll();

		communicator.destroy();
		communicator.init();
		poll();

		Assertions.assertEquals(2, communicator.requests(CALL_LOAD_KPI));
	}

	/**
	 * Every one of the 21 interval properties maps to its own group: setting one caches that group only.
	 */
	@Test
	void testEachIntervalPropertyCachesOnlyItsGroup() throws Exception {
		communicator.init();
		communicator.setRegistrationStatisticsRequestInterval("3600");
		Map<String, String> stats = poll();

		Set<String> groupsWithRetrievedTime = new HashSet<>();
		stats.keySet().stream()
				.filter(key -> key.endsWith(Constant.HASH + Constant.DATA_RETRIEVED_TIME))
				.forEach(key -> groupsWithRetrievedTime.add(key.substring(0, key.indexOf(Constant.HASH))));
		Assertions.assertEquals(Set.of(Constant.REGISTRATION_STATISTICS_GROUP), groupsWithRetrievedTime);
	}
}
