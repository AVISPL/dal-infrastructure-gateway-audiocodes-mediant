/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.security.auth.login.FailedLoginException;

import org.springframework.web.util.UriComponentsBuilder;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.EndpointStatistics;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.Communicator;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.KpiProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Constant;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.DataConversionException;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common.Util;
import com.avispl.symphony.dal.util.ControllablePropertyFactory;
import com.avispl.symphony.dal.util.StringUtils;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.models.Status;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.types.GeneralProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.types.NetworkProperty;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallCapacityStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallLoadStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallMediaIssuesStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallQualityStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallRoutingStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallTerminationStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.CallTrafficStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.MediaClusterStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.MediaDspStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.MediaStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.RegistrationStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.SipRecStatsProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.Alarms;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.AlarmsResponse;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.KpiValue;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.TestCallConfig;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.TestCallDialResponse;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.TestCallStatus;


/**
 * AudioCodesMediantCommunicator class
 *
 * @author Kevin / Symphony Dev Team
 * @since 1.0.0
 */
public class AudioCodesMediantCommunicator extends Communicator implements Monitorable, Controller {
	private final ExtendedStatistics localExtendedStatistics = new ExtendedStatistics();
	private final EndpointStatistics localEndpointStatistics = new EndpointStatistics();
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/** Adapter metadata properties - adapter version and build date */
	private final Properties adapterProperties = new Properties();
	/** Cached device status; defaults to an empty {@link Status} to avoid {@code null} checks. */
	private Status deviceStatus = new Status();

	/** Device adapter instantiation timestamp. */
	private final long adapterInitializationTimestamp = System.currentTimeMillis();

	/**
	 * Cached list containing all active system alarms and alerts reported by the device.
	 */
	private List<Alarms> alarmsList;

	/** Session id of the currently active (or last dialed) SIP test call; {@code null} if none has been dialed. */
	private String callDiagnosticSessionId;
	/** Staged {@code calledNumber} value for the next {@code CallDiagnostic#Start}; set via {@code CallDiagnostic#CalledNumber}. */
	private String callDiagnosticCalledNumber = "";
	/** Staged {@code callingNumber} value for the next {@code CallDiagnostic#Start}; set via {@code CallDiagnostic#CallingNumber}. */
	private String callDiagnosticCallingNumber = "";
	/** Staged {@code destAddress} value for the next {@code CallDiagnostic#Start}; set via {@code CallDiagnostic#Destination}. */
	private String callDiagnosticDestination = "";
	/** Last known status of the SIP test call (device's {@code callStatus}, or a local sentinel before the first dial/after a manual stop). */
	private String callDiagnosticStatus = Constant.CALL_DIAGNOSTICS_NOT_DIALED;
	/** Device-reported {@code callId} of the currently/last tracked test call. */
	private String callDiagnosticCallId = Constant.NOT_AVAILABLE;
	/** Device-reported {@code releaseCause}; only populated once the call is {@code Disconnected}/{@code Failed}. */
	private String callDiagnosticReleaseCause = Constant.NOT_AVAILABLE;

	/**
	 * Names of the optional statistics groups to pull from the device and display: the KPI-heavy
	 * call-statistics groups (e.g. {@code CallLoadStatistics}, {@code CallRoutingStatistics}) and
	 * {@link Constant#CALL_DIAGNOSTICS_GROUP}. Defaults to {@link Constant#CALL_STATS_ALL_GROUPS},
	 * meaning every group is fetched unless the caller narrows the list - each KPI group is a
	 * per-KPI request against the device, and {@code CallDiagnostics} involves its own test-call
	 * session tracking, so groups the caller doesn't ask for are skipped entirely rather than
	 * fetched and hidden. Including {@link Constant#CALL_STATS_ALL_GROUPS} enables every group
	 * regardless of what else is listed.
	 */
	private List<String> displayPropertyGroups = new ArrayList<>(List.of(Constant.CALL_STATS_ALL_GROUPS));

	/**
	 * Property names - the {@code <PropertyName>} portion of a statistics key, with no group prefix but
	 * the unit suffix included (e.g. {@code AnswerSeizureRatio(%)}, {@code MediaJitterIn(ms)}) - to
	 * report as dynamic statistics instead of static ones. A name is matched against the part of each
	 * statistics key after {@link Constant#HASH}, or against the whole key for the ungrouped General
	 * and Network properties, so a name two groups shared would be selected in both - no two groups
	 * currently share one. Matching is otherwise exact - no normalisation, prefix matching or
	 * unit-suffix stripping - so a full {@code <GroupName>#<PropertyName>} key is not a valid entry.
	 * Only the names in {@link Constant#SUPPORTED_HISTORICAL_PROPERTIES} can reach this set; anything
	 * else is dropped by {@link #setHistoricalProperties(String)}, so every entry here corresponds to
	 * a property this adapter can emit.
	 * <p>
	 * Deliberately unlike {@link #displayPropertyGroups}, this fails closed: an unset, blank or
	 * entirely unsupported value leaves the set empty, meaning nothing is reported dynamically and
	 * every property stays in the static statistics map. There is no "select everything" fallback -
	 * moving a property to dynamic statistics changes how Symphony stores and graphs it, so it only
	 * ever happens for properties the caller named explicitly.
	 */
	private Set<String> historicalProperties = new LinkedHashSet<>();

	/**
	 * Jackson ObjectMapper for JSON deserialization. Initialized eagerly in the constructor
	 * to prevent NullPointerException if {@code convertNode} is invoked before {@code internalInit}.
	 */
	private ObjectMapper objectMapper;


	public AudioCodesMediantCommunicator() throws IOException {
		this.localExtendedStatistics.setStatistics(new HashMap<>());
		this.localExtendedStatistics.setDynamicStatistics(new HashMap<>());
		this.localExtendedStatistics.setControllableProperties(new ArrayList<>());
		adapterProperties.load(getClass().getResourceAsStream("/version.properties"));
	}

	public String getDisplayPropertyGroups() {
		return String.join(",", displayPropertyGroups);
	}

	/**
	 * Sets which optional statistics groups (e.g. {@code CallLoadStatistics}, {@code CallRoutingStatistics},
	 * {@link Constant#CALL_DIAGNOSTICS_GROUP}) to pull from the device and display, from a comma-separated
	 * string. Groups not listed here are skipped entirely on every poll cycle - no request is made and no
	 * stats are emitted for them. Passing {@link Constant#CALL_STATS_ALL_GROUPS} (alone or alongside other
	 * names) enables every group. Any name not in {@link Constant#SUPPORTED_PROPERTY_GROUPS} is dropped
	 * (and logged as a warning); if that leaves nothing (every supplied name was unsupported), this falls
	 * back to {@link Constant#CALL_STATS_ALL_GROUPS} - which enables every group - rather than displaying
	 * nothing. That fallback is deliberate but easy to mistake for a working configuration, so it is
	 * logged separately: a single typo is otherwise indistinguishable from asking for everything.
	 *
	 * @param displayPropertyGroups comma-separated group names; blank/empty clears the list (nothing displayed)
	 */
	public void setDisplayPropertyGroups(String displayPropertyGroups) {
		if (StringUtils.isNullOrEmpty(displayPropertyGroups, true)) {
			this.displayPropertyGroups = new ArrayList<>();
			return;
		}
		List<String> requestedGroups = Arrays.stream(displayPropertyGroups.split(","))
				.map(String::strip)
				.filter(group -> !group.isEmpty())
				.collect(Collectors.toList());
		List<String> supportedGroups = requestedGroups.stream()
				.filter(Constant.SUPPORTED_PROPERTY_GROUPS::contains)
				.collect(Collectors.toList());

		List<String> unsupportedGroups = requestedGroups.stream()
				.filter(group -> !Constant.SUPPORTED_PROPERTY_GROUPS.contains(group))
				.collect(Collectors.toList());
		if (!unsupportedGroups.isEmpty()) {
			this.logger.warn("Ignoring unsupported displayPropertyGroups value(s) [%s]; supported values are [%s] (matching is case-sensitive)".formatted(
					String.join(Constant.COMMA, unsupportedGroups),
					Constant.SUPPORTED_PROPERTY_GROUPS.stream().sorted().collect(Collectors.joining(Constant.COMMA))));
		}
		if (supportedGroups.isEmpty()) {
			this.logger.warn("None of the supplied displayPropertyGroups value(s) [%s] is supported; falling back to '%s', which enables every group".formatted(
					String.join(Constant.COMMA, requestedGroups), Constant.CALL_STATS_ALL_GROUPS));
		}
		this.displayPropertyGroups = supportedGroups.isEmpty() ? new ArrayList<>(List.of(Constant.CALL_STATS_ALL_GROUPS)) : supportedGroups;
	}

	public String getHistoricalProperties() {
		return String.join(",", historicalProperties);
	}

	/**
	 * Sets which properties are reported as dynamic (historical) statistics rather than static ones,
	 * from a comma-separated string of property names. Each entry is the property name on its own - no
	 * group prefix and no {@code #} separator, but the unit suffix included, e.g.
	 * {@code AnswerSeizureRatio(%)} - and is matched against the part of each statistics key after
	 * {@link Constant#HASH}, so {@code AnswerSeizureRatio(%)} selects the
	 * {@code CallQualityStatistics#AnswerSeizureRatio(%)} statistic. A name two groups shared would be
	 * selected in both, and the ungrouped General and Network properties, whose keys carry no prefix,
	 * are matched by their whole key. Matching is otherwise exact - no normalisation, prefix matching
	 * or unit-suffix stripping is performed - so a full group-prefixed key is not a valid entry and is
	 * rejected.
	 * <p>
	 * Entries are validated against {@link Constant#SUPPORTED_HISTORICAL_PROPERTIES}: a name not in
	 * that set is dropped (and logged as a warning) rather than retained as a silently inert entry, so
	 * a typo shows up in the logs instead of quietly producing no graph. Unlike
	 * {@link #setDisplayPropertyGroups(String)}, dropping every supplied name does <em>not</em> fall
	 * back to selecting everything: this fails closed, so a blank value, or one whose every entry is
	 * unsupported, yields no dynamic statistics at all and leaves every property in the static map.
	 * <p>
	 * Note that a listed name is only moved if the property is actually present in a given cycle, so
	 * naming a property from a group excluded by {@link #displayPropertyGroups} has no effect.
	 *
	 * @param historicalProperties comma-separated property names, without group prefix, each one of
	 * {@link Constant#SUPPORTED_HISTORICAL_PROPERTIES}; unsupported names are dropped, blank/empty selects nothing
	 */
	public void setHistoricalProperties(String historicalProperties) {
		if (StringUtils.isNullOrEmpty(historicalProperties, true)) {
			this.historicalProperties = new LinkedHashSet<>();
			return;
		}
		List<String> requestedProperties = Arrays.stream(historicalProperties.split(","))
				.map(String::strip)
				.filter(name -> !name.isEmpty())
				.collect(Collectors.toList());

		List<String> unsupportedProperties = requestedProperties.stream()
				.filter(name -> !Constant.SUPPORTED_HISTORICAL_PROPERTIES.contains(name))
				.collect(Collectors.toList());
		if (!unsupportedProperties.isEmpty()) {
			this.logger.warn("Ignoring unsupported historicalProperties value(s) [%s]; supported values are [%s] (matching is case-sensitive)".formatted(
					String.join(Constant.COMMA, unsupportedProperties),
					Constant.SUPPORTED_HISTORICAL_PROPERTIES.stream().sorted().collect(Collectors.joining(Constant.COMMA))));
		}

		this.historicalProperties = requestedProperties.stream()
				.filter(Constant.SUPPORTED_HISTORICAL_PROPERTIES::contains)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Whether {@code groupName} should be fetched/displayed this cycle - either it's explicitly
	 * listed in {@link #displayPropertyGroups}, or {@link Constant#CALL_STATS_ALL_GROUPS} is.
	 *
	 * @param groupName the group to check
	 * @return {@code true} if the group is enabled
	 */
	private boolean isGroupEnabled(String groupName) {
		return this.displayPropertyGroups.contains(Constant.CALL_STATS_ALL_GROUPS) || this.displayPropertyGroups.contains(groupName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}
		this.alarmsList = new ArrayList<>();
		this.objectMapper = new ObjectMapper();
		this.setTrustAllCertificates(true);
		super.internalInit();
	}

	@Override
	protected void internalDestroy() {
		//  Clear the extended properties
		this.localExtendedStatistics.setStatistics(new HashMap<>());
		this.localExtendedStatistics.setDynamicStatistics(new HashMap<>());
		this.localExtendedStatistics.setControllableProperties(new ArrayList<>());
		//	Clear the populated data
		this.adapterProperties.clear();
		this.deviceStatus = new Status();
		this.callDiagnosticSessionId = null;
		clearCallDiagnosticConfig();
		this.callDiagnosticStatus = Constant.CALL_DIAGNOSTICS_NOT_DIALED;
		this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
		this.callDiagnosticReleaseCause = Constant.NOT_AVAILABLE;
		super.internalDestroy();
	}

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		reentrantLock.lock();
		try {
			Map<String, String> stats = new HashMap<>();
			List<AdvancedControllableProperty> controls = new ArrayList<>();
			retrieveMetadata(stats);
			retrieveDeviceStatus(stats);
			retrieveKpiGroups(stats);
			retrieveCallDiagnostics(stats, controls);
			if (isGroupEnabled(Constant.ACTIVE_ALARM)) {
				this.setupData();
				stats.putAll(Util.generateActiveAlarmsProperties(this.alarmsList));
			}
			Map<String, String> dynamicStats = extractHistoricalProperties(stats);
			this.localExtendedStatistics.setStatistics(stats);
			this.localExtendedStatistics.setDynamicStatistics(dynamicStats);
			this.localExtendedStatistics.setControllableProperties(controls);
		} finally {
			reentrantLock.unlock();
		}
		return List.of(this.localExtendedStatistics, this.localEndpointStatistics);
	}

	/**
	 * Copies every collected property whose name is listed in {@link #historicalProperties} and whose
	 * value is numeric into a freshly allocated map, to be reported as dynamic statistics. The
	 * selection is by property name alone: what is looked up is the part of each statistics key after
	 * {@link Constant#HASH}, or the whole key for the ungrouped General and Network properties, and
	 * each match is keyed in the returned map by its full {@code <GroupName>#<PropertyName>} statistics
	 * key. A listed name matching nothing collected this cycle - because its group is excluded by
	 * {@link #displayPropertyGroups} - simply contributes nothing; a misspelled or group-prefixed name
	 * cannot reach here, having been rejected by {@link #setHistoricalProperties(String)}.
	 * <p>
	 * {@code stats} is never modified: a selected property is reported as an extended property in
	 * every case, carrying either the device's value or {@link Constant#NOT_AVAILABLE} when there is
	 * none, and is <em>additionally</em> reported as a dynamic statistic whenever that value is
	 * numeric (see {@link Util#isNumeric(String)}). A selected property is therefore expected to
	 * appear in both maps in the same cycle - that duplication is intentional, so that an operator
	 * reading the device panel sees the property whether or not it currently has a graphable value.
	 * <p>
	 * Dynamic statistics are stored and graphed as a time series, so a non-numeric value - most often
	 * {@link Constant#NOT_AVAILABLE}, emitted whenever a KPI request fails or the device reports an
	 * empty value - is not copied across: a gap in the series reflects that the device did not report
	 * a usable value, while the extended property still shows what it did report. Such a value is
	 * logged at warn level with its key and value, since a property deliberately selected for
	 * graphing that yields no data point is worth spotting in the logs - whether the device is failing
	 * to report it.
	 * <p>
	 * Iteration is over {@code stats} rather than {@link #historicalProperties} because a listed name
	 * is no longer a key that can be looked up directly - it identifies however many keys end in that
	 * property name. A new map is returned each call rather than one being reused, so a property that
	 * stops being collected does not linger from a previous cycle.
	 *
	 * <p>
	 * Package-private rather than private so it can be exercised directly, without a device: the
	 * only public route to it is {@link #getMultipleStatistics()}, which polls first.
	 *
	 * @param stats the statistics collected this cycle; left unmodified
	 * @return the properties to report as dynamic statistics, keyed by their full statistics key;
	 * empty if none were selected, matched or numeric
	 */
	Map<String, String> extractHistoricalProperties(Map<String, String> stats) {
		Map<String, String> dynamicStats = new HashMap<>();
		if (this.historicalProperties.isEmpty()) {
			return dynamicStats;
		}
		for (Map.Entry<String, String> statistic : stats.entrySet()) {
			String key = statistic.getKey();
			int hashIndex = key.lastIndexOf(Constant.HASH);
			String propertyName = hashIndex < 0 ? key : key.substring(hashIndex + Constant.HASH.length());
			if (!this.historicalProperties.contains(propertyName)) {
				continue;
			}
			String value = statistic.getValue();
			if (Util.isNumeric(value)) {
				dynamicStats.put(key, value);
			} else {
				this.logger.warn("The '%s' property is selected as a historical property but its value '%s' is not numeric; reporting it as an extended property only".formatted(key, value));
			}
		}
		return dynamicStats;
	}

	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		reentrantLock.lock();
		try {
			String property = controllableProperty.getProperty();
			Object value = controllableProperty.getValue();
			String calledNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLED_NUMBER;
			String callingNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLING_NUMBER;
			String destinationKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_DESTINATION;
			String statusKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STATUS;
			String callIdKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALL_ID;
			String releaseCauseKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_RELEASE_CAUSE;
			String startKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_START;
			String stopKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STOP;

			if (calledNumberKey.equals(property)) {
				this.callDiagnosticCalledNumber = validateMaxLength(String.valueOf(value), Constant.CALL_DIAGNOSTICS_CALLED_NUMBER, Constant.CALL_DIAGNOSTICS_CALLED_NUMBER_MAX_LENGTH);
			} else if (callingNumberKey.equals(property)) {
				this.callDiagnosticCallingNumber = validateMaxLength(String.valueOf(value), Constant.CALL_DIAGNOSTICS_CALLING_NUMBER, Constant.CALL_DIAGNOSTICS_CALLING_NUMBER_MAX_LENGTH);
			} else if (destinationKey.equals(property)) {
				this.callDiagnosticDestination = validateMaxLength(String.valueOf(value), Constant.CALL_DIAGNOSTICS_DESTINATION, Constant.CALL_DIAGNOSTICS_DESTINATION_MAX_LENGTH);
			} else if (startKey.equals(property)) {
				startCallDiagnostic();
			} else if (stopKey.equals(property)) {
				stopCallDiagnostic();
			} else {
				throw new IllegalArgumentException("Unsupported control property: '%s'".formatted(property));
			}

			//	Reflect the change in the live stats map immediately rather than waiting for the next poll cycle.
			Map<String, String> liveStats = this.localExtendedStatistics.getStatistics();
			liveStats.put(calledNumberKey, this.callDiagnosticCalledNumber);
			liveStats.put(callingNumberKey, this.callDiagnosticCallingNumber);
			liveStats.put(destinationKey, this.callDiagnosticDestination);
			liveStats.put(statusKey, this.callDiagnosticStatus);
			liveStats.put(callIdKey, this.callDiagnosticCallId);
			liveStats.put(releaseCauseKey, this.callDiagnosticReleaseCause);

			List<AdvancedControllableProperty> liveControls = this.localExtendedStatistics.getControllableProperties();
			liveControls.removeIf(control -> stopKey.equals(control.getName()));
			if (isCallDiagnosticStoppable()) {
				liveStats.put(stopKey, "");
				liveControls.add(ControllablePropertyFactory.createButton(stopKey, "Stop", "Stopping...", 0L));
			} else {
				liveStats.remove(stopKey);
			}
		} finally {
			reentrantLock.unlock();
		}
	}

	@Override
	public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
		if (controllableProperties == null) {
			return;
		}
		for (ControllableProperty controllableProperty : controllableProperties) {
			controlProperty(controllableProperty);
		}
	}

	/**
	 * Rejects a staged {@code CallDiagnostics} value that exceeds the device's {@code sipTestCall/dial}
	 * length limit for the given field.
	 *
	 * @param value     the staged value to validate
	 * @param fieldName the field's display name, for the error message
	 * @param maxLength the field's maximum allowed length
	 * @return {@code value}, unchanged
	 * @throws IllegalArgumentException if {@code value} exceeds {@code maxLength}
	 */
	private String validateMaxLength(String value, String fieldName, int maxLength) {
		if (value.length() > maxLength) {
			throw new IllegalArgumentException("%s must not exceed %d characters".formatted(fieldName, maxLength));
		}
		return value;
	}

	/**
	 * Retrieves metadata information and updates the provided statistics and dynamic map.
	 *
	 * @param stats the map where statistics will be stored
	 */
	private void retrieveMetadata(Map<String, String> stats) {
		try {
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_VERSION,
					Util.getDefaultValueForNullData(adapterProperties.getProperty("adapter.version")));
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_BUILD_DATE,
					Util.getDefaultValueForNullData(adapterProperties.getProperty("adapter.build.date")));

			long adapterUptime = System.currentTimeMillis() - adapterInitializationTimestamp;
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_UPTIME, Util.formatUpTime(adapterUptime / 1000));
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_UPTIME_MIN, String.valueOf(adapterUptime / (1000 * 60)));

			String activePropertyGroups = this.displayPropertyGroups.stream()
					.sorted()
					.collect(Collectors.joining(Constant.COMMA));
			stats.put(Constant.ADAPTER_METADATA + Constant.HASH + Constant.ADAPTER_ACTIVE_PROPERTY_GROUPS,
					StringUtils.isNotNullOrEmpty(activePropertyGroups) ? activePropertyGroups : Constant.NOT_AVAILABLE);
		} catch (Exception e) {
			logger.error("Failed to populate metadata information", e);
		}
	}

	/**
	 * Sends a GET request to {@code uri} and returns the raw {@link JsonNode}, or {@code null}
	 * if the device responded with no content (e.g. an empty body).
	 * <p>
	 * {@link FailedLoginException} and {@link ResourceNotReachableException} - the SDK's own typed
	 * signals for "bad credentials" and "device unreachable" respectively - are propagated as-is so
	 * Symphony's UI surfaces the accurate error.
	 *
	 * @param uri the API endpoint to call
	 * @return the raw response node, or {@code null} if the response body was empty
	 * @throws FailedLoginException          if authentication fails
	 * @throws ResourceNotReachableException if the device cannot be reached
	 */
	private JsonNode fetchJsonNode(String uri) throws FailedLoginException {
		try {
			return super.doGet(uri, JsonNode.class);
		} catch (FailedLoginException | ResourceNotReachableException e) {
			throw e;
		} catch (Exception e) {
			this.logger.error(Constant.FETCH_DATA_FAILED.formatted(uri, JsonNode.class.getName()), e);
			throw new IllegalStateException("Failed to send a request 'GET %s'".formatted(uri), e);
		}
	}

	/**
	 * Fetches JSON from {@code uri} and deserializes it into {@code targetClass}.
	 * <p>
	 * Returns {@code null} (with a warning log) if the device response is empty/no-content. This is
	 * treated as a confirmed, authoritative "no data" signal from the device rather than an error -
	 * callers should generally act on it (e.g. clear cached state), not just ignore it.
	 * <p>
	 * Throws {@link DataConversionException} if a response body was received but could not be
	 * deserialized into {@code targetClass} (malformed/unexpected payload) - this represents a
	 * genuine error, and callers should generally retain their previously cached value instead of
	 * overwriting it with incomplete data.
	 *
	 * @param <T>         the target type
	 * @param uri         the API endpoint to call
	 * @param targetClass the class to deserialize into
	 * @return the deserialized object, or {@code null} if the device returned no content
	 * @throws FailedLoginException    if the HTTP request itself fails
	 * @throws DataConversionException if the response body could not be deserialized
	 */
	private <T> T fetchAndConvert(String uri, Class<T> targetClass) throws FailedLoginException, DataConversionException {
		String responseClassName = targetClass.getName();
		JsonNode node = fetchJsonNode(uri);
		if (node == null) {
			this.logger.warn(Constant.FETCHED_DATA_NULL_WARNING.formatted(uri, responseClassName));
			return null;
		}
		try {
			return this.objectMapper.convertValue(node, targetClass);
		} catch (IllegalArgumentException e) {
			String message = Constant.CONVERT_DATA_FAILED.formatted(responseClassName);
			this.logger.error(message, e);
			throw new DataConversionException(message, e);
		}
	}

	/**
	 * Fetches and refreshes the cached active-alarms list from the device.
	 * <p>
	 * A {@code null} response (no content) or an empty/absent {@code alarms} array are both
	 * treated as an explicit, authoritative signal from the device that there are currently zero
	 * active alarms, and {@link #alarmsList} is reset to an empty list accordingly. This prevents
	 * Symphony from continuing to display a stale alarm count/details after all alarms have been
	 * cleared on the device.
	 * <p>
	 * If the top-level response cannot be parsed at all ({@link DataConversionException}), the
	 * previously cached {@link #alarmsList} is retained rather than risking data loss based on a
	 * malformed payload. The same applies per-alarm: if a specific alarm's detail response cannot
	 * be parsed, that alarm is skipped (logged) without discarding the rest of the refreshed list.
	 *
	 * @throws FailedLoginException if the HTTP request fails due to authentication or connectivity issues
	 */
	private void setupData() throws Exception {
		AlarmsResponse alarmsResponse;
		try {
			alarmsResponse = fetchAndConvert(Constant.ACTIVE_ALARMS_API, AlarmsResponse.class);
		} catch (DataConversionException e) {
			//	Malformed/unexpected payload - keep the previously cached alarms list.
			this.logger.warn("Retaining previously cached alarms list due to a malformed response from %s".formatted(Constant.ACTIVE_ALARMS_API), e);
			return;
		}

		List<Alarms> alarmRefs = alarmsResponse == null ? null : alarmsResponse.getAlarms();
		if (alarmRefs == null || alarmRefs.isEmpty()) {
			//	No content, or an empty/absent "alarms" array - the device is reporting zero active alarms.
			this.alarmsList = new ArrayList<>();
			return;
		}

		List<Alarms> fullAlarmsList = new ArrayList<>();
		for (Alarms alarm : alarmRefs) {
			try {
				Alarms fullAlarm = fetchAndConvert(Constant.ACTIVE_ALARMS_API + Constant.SLASH + alarm.getId(), Alarms.class);
				if (fullAlarm != null) {
					fullAlarmsList.add(fullAlarm);
				}
			} catch (DataConversionException e) {
				this.logger.error("Skipping alarm '%s' due to a malformed detail response.".formatted(alarm.getId()), e);
			}
		}
		this.alarmsList = fullAlarmsList;
	}

	/**
	 * Retrieves the device status from the remote endpoint and populates {@code stats}
	 * with properties from the {@code General} and {@code Network} groups.
	 *
	 * <p>If the status response is {@code null}, a warning is logged and both groups are skipped entirely.
	 *
	 * <p>{@link GeneralProperty} values are populated with a default fallback for {@code null} data;
	 * {@link NetworkProperty} values use a stricter fallback (see {@link Util#getDefaultValueForNullData(String, boolean)}).
	 *
	 * @param stats the map to populate with property display names as keys
	 * and their corresponding string values as values; must not be {@code null}
	 * @throws Exception if the status request fails for any other reason
	 */
	private void retrieveDeviceStatus(Map<String, String> stats) throws Exception {
		this.deviceStatus = this.fetchData(Status.class, Constant.STATUS_ENDPOINT);
		if (deviceStatus == null) {
			log.warn("The device status param is null; skip retrieving the General and Network group");
			return;
		}
		Arrays.stream(GeneralProperty.values()).forEach(property -> {
			var value = GeneralProperty.getPropertyValue(property, deviceStatus);
			stats.put(property.getDisplayName(), Util.getDefaultValueForNullData(value));
		});
		if (isGroupEnabled(Constant.NETWORK_GROUP)) {
			Arrays.stream(NetworkProperty.values()).forEach(property -> {
				var value = NetworkProperty.getPropertyValue(property, deviceStatus);
				stats.put(property.getDisplayName(), Util.getDefaultValueForNullData(value, false));
			});
		}
	}

	/**
	 * Retrieves each optional KPI group whose name is present in {@link #displayPropertyGroups}, populating
	 * {@code stats} with a {@code <GroupName>#<PropertyName>} entry per KPI. Groups not listed there are
	 * skipped entirely - no request is made for any of their KPIs.
	 * <p>
	 * Each group is paired with the device KPI scope backing it. Most scopes map one-to-one onto a display
	 * group, except {@link Constant#CALL_STATS_KPI_API}, which backs all seven {@code Call*Statistics}
	 * groups, and {@link Constant#SBC_OTHER_STATS_KPI_API}, which backs
	 * {@link Constant#REGISTRATION_STATISTICS_GROUP}.
	 *
	 * @param stats the map to populate with property display names as keys
	 * and their corresponding string values as values; must not be {@code null}
	 * @throws FailedLoginException if a KPI request fails due to authentication issues
	 */
	private void retrieveKpiGroups(Map<String, String> stats) throws FailedLoginException {
		retrieveKpiGroup(stats, Constant.CALL_STATS_KPI_API, Constant.CALL_LOAD_STATISTICS_GROUP, CallLoadStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_STATS_KPI_API, Constant.CALL_QUALITY_STATISTICS_GROUP, CallQualityStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_STATS_KPI_API, Constant.CALL_TERMINATION_STATISTICS_GROUP, CallTerminationStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_STATS_KPI_API, Constant.CALL_MEDIA_ISSUES_STATISTICS_GROUP, CallMediaIssuesStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_STATS_KPI_API, Constant.CALL_CAPACITY_STATISTICS_GROUP, CallCapacityStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_STATS_KPI_API, Constant.CALL_ROUTING_STATISTICS_GROUP, CallRoutingStatsProperty.values());
		retrieveKpiGroup(stats, Constant.CALL_STATS_KPI_API, Constant.CALL_TRAFFIC_STATISTICS_GROUP, CallTrafficStatsProperty.values());
		retrieveKpiGroup(stats, Constant.MEDIA_STATS_KPI_API, Constant.MEDIA_STATISTICS_GROUP, MediaStatsProperty.values());
		retrieveKpiGroup(stats, Constant.MEDIA_DSP_STATS_KPI_API, Constant.MEDIA_DSP_STATISTICS_GROUP, MediaDspStatsProperty.values());
		retrieveKpiGroup(stats, Constant.MEDIA_CLUSTER_STATS_KPI_API, Constant.MEDIA_CLUSTER_STATISTICS_GROUP, MediaClusterStatsProperty.values());
		retrieveKpiGroup(stats, Constant.SBC_OTHER_STATS_KPI_API, Constant.REGISTRATION_STATISTICS_GROUP, RegistrationStatsProperty.values());
		retrieveKpiGroup(stats, Constant.SIP_REC_STATS_KPI_API, Constant.SIP_REC_STATISTICS_GROUP, SipRecStatsProperty.values());
	}

	/**
	 * Retrieves every {@code property} belonging to {@code groupName} from the {@code scopeApi} KPI scope,
	 * populating {@code stats} with a {@code <groupName>#<PropertyName>} entry for each - but only if
	 * {@code groupName} (or {@link Constant#CALL_STATS_ALL_GROUPS}) is present in
	 * {@link #displayPropertyGroups}; otherwise this is a no-op and no request is made.
	 * <p>
	 * Each KPI is fetched independently via its own request. A {@code null} response (no content) is
	 * reported as {@link Constant#NOT_AVAILABLE}, not as a zero: the device declining to supply a value
	 * is not the same as it reporting a value of zero, and recording the former as the latter would put a
	 * fabricated data point into the time series of a KPI selected as a historical property. Any failure
	 * to fetch or parse a given KPI - a malformed response ({@link DataConversionException}) or any other
	 * error (e.g. the device not recognizing this particular {@code kpiId}) - is reported the same way,
	 * and is logged, without affecting the rest of the group or aborting the poll cycle.
	 *
	 * @param <T>        the {@link KpiProperty} enum type for this group
	 * @param stats      the map to populate with property display names as keys
	 * @param scopeApi   the device KPI scope backing this group; the {@code kpiId} is appended to it
	 * @param groupName  the group name gating and prefixing these properties
	 * @param properties the KPI properties belonging to this group
	 * @throws FailedLoginException if a KPI request fails due to authentication issues
	 */
	private <T extends Enum<T> & KpiProperty> void retrieveKpiGroup(Map<String, String> stats, String scopeApi, String groupName, T[] properties)
			throws FailedLoginException {
		if (!isGroupEnabled(groupName)) {
			return;
		}
		for (T property : properties) {
			String uri = scopeApi + Constant.SLASH + property.getKpiId();
			String value;
			try {
				KpiValue kpi = fetchAndConvert(uri, KpiValue.class);
				value = kpi == null ? null : kpi.getValue();
			} catch (DataConversionException e) {
				this.logger.error("Failed to parse the '%s' KPI response from %s".formatted(property.getKpiId(), uri), e);
				value = null;
			} catch (FailedLoginException e) {
				throw e;
			} catch (Exception e) {
				this.logger.error("Failed to fetch the '%s' KPI from %s".formatted(property.getKpiId(), uri), e);
				value = null;
			}
			stats.put(groupName + Constant.HASH + property.getName(), Util.getDefaultValueForNullData(value, false));
		}
	}

	/**
	 * Sends a POST request to {@code uri} with {@code requestBody} and deserializes the response into {@code targetClass}.
	 * <p>
	 * {@link FailedLoginException} and {@link ResourceNotReachableException} are propagated as-is, matching
	 * {@link #fetchJsonNode(String)}'s contract.
	 *
	 * @param <T>         the target type
	 * @param uri         the API endpoint to call
	 * @param requestBody the request body to serialize and send
	 * @param targetClass the class to deserialize the response into
	 * @return the deserialized response object
	 * @throws FailedLoginException if authentication fails
	 */
	private <T> T postAndConvert(String uri, Object requestBody, Class<T> targetClass) throws FailedLoginException {
		try {
			return super.doPost(uri, requestBody, targetClass);
		} catch (FailedLoginException | ResourceNotReachableException e) {
			throw e;
		} catch (Exception e) {
			this.logger.error("Exception while posting data. Endpoint: %s".formatted(uri), e);
			throw new IllegalStateException("Failed to send a request 'POST %s'".formatted(uri), e);
		}
	}

	/**
	 * Populates {@code stats} with the current {@code CallDiagnostics} group values and appends the
	 * corresponding controls ({@code CalledNumber}/{@code CallingNumber}/{@code Destination} text
	 * fields and {@code Start}/{@code Stop} buttons) to {@code controls}. Like the KPI-heavy statistics
	 * groups, the whole {@code CallDiagnostics} group is gated behind {@link #displayPropertyGroups}
	 * (or {@link Constant#CALL_STATS_ALL_GROUPS}); if it isn't listed, this is a no-op and no request
	 * is made, even for a session already in progress.
	 * <p>
	 * If a test call session is currently tracked, its status is re-fetched from the device first
	 * (see {@link #refreshCallDiagnosticStatus()}) so that a call which disconnected on its own
	 * (rather than via the {@code Stop} control) is not reported as stale/{@code Connected} forever.
	 * <p>
	 * The {@code Stop} button/property is only included while there's actually something to stop
	 * (see {@link #isCallDiagnosticStoppable()}) - not before the first dial, and not once the call is over.
	 *
	 * @param stats    the map to populate with property display names as keys
	 * @param controls the list to append the {@code CallDiagnostics} controls to
	 * @throws FailedLoginException if refreshing the test call status fails due to authentication issues
	 */
	private void retrieveCallDiagnostics(Map<String, String> stats, List<AdvancedControllableProperty> controls) throws FailedLoginException {
		if (!isGroupEnabled(Constant.CALL_DIAGNOSTICS_GROUP)) {
			return;
		}
		if (this.callDiagnosticSessionId != null) {
			refreshCallDiagnosticStatus();
		}

		String calledNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLED_NUMBER;
		String callingNumberKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALLING_NUMBER;
		String destinationKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_DESTINATION;
		String statusKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STATUS;
		String callIdKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_CALL_ID;
		String releaseCauseKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_RELEASE_CAUSE;
		String startKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_START;
		String stopKey = Constant.CALL_DIAGNOSTICS_GROUP + Constant.HASH + Constant.CALL_DIAGNOSTICS_STOP;

		stats.put(calledNumberKey, this.callDiagnosticCalledNumber);
		stats.put(callingNumberKey, this.callDiagnosticCallingNumber);
		stats.put(destinationKey, this.callDiagnosticDestination);
		stats.put(statusKey, this.callDiagnosticStatus);
		stats.put(callIdKey, this.callDiagnosticCallId);
		stats.put(releaseCauseKey, this.callDiagnosticReleaseCause);
		//	Buttons require a paired statistics entry to render, even though they carry no persistent value.
		stats.put(startKey, "");

		controls.add(ControllablePropertyFactory.createText(calledNumberKey, this.callDiagnosticCalledNumber));
		controls.add(ControllablePropertyFactory.createText(callingNumberKey, this.callDiagnosticCallingNumber));
		controls.add(ControllablePropertyFactory.createText(destinationKey, this.callDiagnosticDestination));
		controls.add(ControllablePropertyFactory.createButton(startKey, "Start", "Starting...", 0L));
		if (isCallDiagnosticStoppable()) {
			stats.put(stopKey, "");
			controls.add(ControllablePropertyFactory.createButton(stopKey, "Stop", "Stopping...", 0L));
		}
	}

	/**
	 * Re-fetches the current test call's status from the device, keeping {@link #callDiagnosticStatus},
	 * {@link #callDiagnosticCallId} and {@link #callDiagnosticReleaseCause} in sync rather than frozen
	 * at whatever they were when the call was dialed.
	 * <p>
	 * A {@code null}/no-content response, or any error other than a login failure (e.g. a 404 once the
	 * device has expired/forgotten the session), is treated as an authoritative signal that the call is
	 * over: {@link #callDiagnosticStatus} is reset to {@link Constant#CALL_DIAGNOSTICS_DISCONNECTED} and
	 * {@link #callDiagnosticSessionId} is cleared, same as a manual {@link #stopCallDiagnostic()}. Once
	 * the status is {@code Disconnected} - whether reported by the device or inferred here -
	 * {@link #callDiagnosticCallId} and {@link #callDiagnosticReleaseCause} are reset to
	 * {@link Constant#NOT_AVAILABLE} too, since neither is being reported by the device for this session
	 * any more (the device only reports {@code releaseCause} for as long as it keeps the session around,
	 * per its {@code keepResultTimeout}) - both stay in sync with what the device is currently reporting
	 * rather than freezing at their last known value.
	 * {@link #callDiagnosticCalledNumber}/{@link #callDiagnosticCallingNumber}/{@link #callDiagnosticDestination}
	 * are deliberately left untouched here too - once a call ends they keep showing what was dialed
	 * until the caller stages new values or the adapter is destroyed, rather than blanking out on
	 * disconnect. A malformed-but-present response is logged and otherwise ignored, leaving all previous
	 * values intact for this cycle.
	 *
	 * @throws FailedLoginException if authentication fails
	 */
	private void refreshCallDiagnosticStatus() throws FailedLoginException {
		String statusUri = UriComponentsBuilder.fromUriString(Constant.TEST_CALL_STATUS_API)
				.queryParam(Constant.SESSION_ID_PARAM, this.callDiagnosticSessionId)
				.build().toUriString();
		try {
			TestCallStatus status = fetchAndConvert(statusUri, TestCallStatus.class);
			if (status == null || status.getCallStatus() == null) {
				this.callDiagnosticStatus = Constant.CALL_DIAGNOSTICS_DISCONNECTED;
				this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
				this.callDiagnosticReleaseCause = Constant.NOT_AVAILABLE;
				this.callDiagnosticSessionId = null;
				return;
			}
			this.callDiagnosticStatus = status.getCallStatus();
			if (Constant.CALL_DIAGNOSTICS_DISCONNECTED.equals(this.callDiagnosticStatus)) {
				this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
			} else {
				this.callDiagnosticCallId = Util.getDefaultValueForNullData(status.getCallId(), false);
				refreshCallDiagnosticConfig();
			}
			this.callDiagnosticReleaseCause = Util.getDefaultValueForNullData(status.getReleaseCause(), false);
		} catch (DataConversionException e) {
			this.logger.error("Failed to parse test call status response from %s".formatted(statusUri), e);
		} catch (FailedLoginException e) {
			throw e;
		} catch (Exception e) {
			this.logger.warn("Failed to refresh test call status from %s; assuming the session has ended".formatted(statusUri), e);
			this.callDiagnosticStatus = Constant.CALL_DIAGNOSTICS_DISCONNECTED;
			this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
			this.callDiagnosticReleaseCause = Constant.NOT_AVAILABLE;
			this.callDiagnosticSessionId = null;
		}
	}

	/**
	 * Fetches the device's own record of the dialed-call parameters ({@code calledNumber}/
	 * {@code callingNumber}/{@code destAddress}) for the current test call session via
	 * {@code GET /sipTestCall/show}, and updates {@link #callDiagnosticCalledNumber}/
	 * {@link #callDiagnosticCallingNumber}/{@link #callDiagnosticDestination} to match, so these
	 * reflect the device's authoritative data rather than staying frozen at whatever was staged
	 * locally before {@code Start} was pressed. A missing/malformed response leaves the
	 * previously known values intact for this cycle rather than blanking them out.
	 *
	 * @throws FailedLoginException if the request fails due to authentication issues
	 */
	private void refreshCallDiagnosticConfig() throws FailedLoginException {
		String showUri = UriComponentsBuilder.fromUriString(Constant.TEST_CALL_SHOW_API)
				.queryParam(Constant.SESSION_ID_PARAM, this.callDiagnosticSessionId)
				.build().toUriString();
		try {
			TestCallConfig config = fetchAndConvert(showUri, TestCallConfig.class);
			if (config == null) {
				return;
			}
			this.callDiagnosticCalledNumber = config.getCalledNumber() != null ? config.getCalledNumber() : "";
			this.callDiagnosticCallingNumber = config.getCallingNumber() != null ? config.getCallingNumber() : "";
			this.callDiagnosticDestination = config.getDestAddress() != null ? config.getDestAddress() : "";
		} catch (DataConversionException e) {
			this.logger.error("Failed to parse test call config response from %s".formatted(showUri), e);
		} catch (FailedLoginException e) {
			throw e;
		} catch (Exception e) {
			this.logger.warn("Failed to refresh test call config from %s; keeping previously known values".formatted(showUri), e);
		}
	}

	/**
	 * Blanks the staged/echoed {@code CalledNumber}/{@code CallingNumber}/{@code Destination}
	 * values. Only called from {@link #internalDestroy()} - once a test call ends, these are
	 * deliberately left in place (reflecting the last dialed/echoed values) until the caller
	 * stages new ones via {@link #controlProperty} or the adapter instance is destroyed.
	 */
	private void clearCallDiagnosticConfig() {
		this.callDiagnosticCalledNumber = "";
		this.callDiagnosticCallingNumber = "";
		this.callDiagnosticDestination = "";
	}

	/**
	 * Dials a new SIP test call using the currently staged {@link #callDiagnosticCalledNumber}/
	 * {@link #callDiagnosticCallingNumber}/{@link #callDiagnosticDestination} values, then immediately
	 * delegates to {@link #refreshCallDiagnosticStatus()} to capture the initial state.
	 * <p>
	 * Per the device's {@code POST /sipTestCall/dial} contract, {@code calledNumber} and
	 * {@code callingNumber} are always required, and a destination is required via either
	 * {@code destAddress} or {@code destIpGroup} - this adapter only exposes the former
	 * ({@code CallDiagnostic#Destination}), so all three staged fields are required here.
	 *
	 * @throws IllegalArgumentException if any of the three staged fields is blank
	 * @throws Exception                if the dial request fails, or returns no session id
	 */
	private void startCallDiagnostic() throws Exception {
		if (StringUtils.isNullOrEmpty(this.callDiagnosticCalledNumber, true)
				|| StringUtils.isNullOrEmpty(this.callDiagnosticCallingNumber, true)
				|| StringUtils.isNullOrEmpty(this.callDiagnosticDestination, true)) {
			throw new IllegalArgumentException("CalledNumber, CallingNumber and Destination must all be set before starting a test call");
		}
		Map<String, Object> body = new HashMap<>();
		body.put("calledNumber", this.callDiagnosticCalledNumber);
		body.put("callingNumber", this.callDiagnosticCallingNumber);
		body.put("destAddress", this.callDiagnosticDestination);

		TestCallDialResponse dialResponse = postAndConvert(Constant.TEST_CALL_DIAL_API, body, TestCallDialResponse.class);
		if (dialResponse == null || dialResponse.getSessionId() == null) {
			throw new IllegalStateException("Test call dial request returned no session id");
		}
		this.callDiagnosticSessionId = String.valueOf(dialResponse.getSessionId());
		//	Clear out the previous call's residual callId/releaseCause so they aren't mistaken for this new call's data.
		this.callDiagnosticCallId = Constant.NOT_AVAILABLE;
		this.callDiagnosticReleaseCause = Constant.NOT_AVAILABLE;

		refreshCallDiagnosticStatus();
	}

	/**
	 * Drops the currently active test call session, if any, then re-fetches its status from the
	 * device via {@link #refreshCallDiagnosticStatus()} - rather than assuming/hardcoding the result -
	 * so {@link #callDiagnosticReleaseCause} picks up the device's actual post-drop release cause
	 * (e.g. {@code RELEASE_BECAUSE_MANUAL_DISC}) exactly like a call the device disconnected on its own.
	 *
	 * @throws Exception if the drop request fails
	 */
	private void stopCallDiagnostic() throws Exception {
		if (this.callDiagnosticSessionId == null) {
			this.logger.warn("No active test call session to stop");
			return;
		}
		String dropUri = UriComponentsBuilder.fromUriString(Constant.TEST_CALL_DROP_API)
				.queryParam(Constant.SESSION_ID_PARAM, this.callDiagnosticSessionId)
				.build().toUriString();
		this.performDelete(dropUri);
		refreshCallDiagnosticStatus();
	}

	/**
	 * Whether there's currently a test call to stop - i.e. {@link #callDiagnosticStatus} is neither the
	 * pre-dial sentinel ({@link Constant#CALL_DIAGNOSTICS_NOT_DIALED}) nor a terminal one
	 * ({@link Constant#CALL_DIAGNOSTICS_DISCONNECTED}). Gates whether the {@code Stop} button/property
	 * is shown.
	 *
	 * @return {@code true} if the {@code Stop} control should be shown
	 */
	private boolean isCallDiagnosticStoppable() {
		return !Constant.CALL_DIAGNOSTICS_NOT_DIALED.equals(this.callDiagnosticStatus)
				&& !Constant.CALL_DIAGNOSTICS_DISCONNECTED.equals(this.callDiagnosticStatus);
	}
}
