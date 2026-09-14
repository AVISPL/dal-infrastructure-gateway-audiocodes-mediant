# AudioCodes Mediant Integration - Capabilities & Configuration
This document covers the AudioCodes Mediant Adapter Capabilities and Configuration.

Symphony integrates with AudioCodes Mediant Session Border Controllers and Media Gateways to provide monitoring of device health, call and media performance, SIP registration activity, and active alarms, along with the ability to run SIP diagnostic test calls on demand.
Main features are: device status and network monitoring, call performance and quality statistics, media and DSP statistics, registration and SIP recording statistics, active alarm visibility, and on-demand SIP test calls.

## Main use cases for AudioCodes Mediant
- **Monitor** device health and status, including operational state, software version, uptime, high-availability state, and pending save/reset/upgrade actions
- **Monitor** call performance, including call load, capacity, routing, traffic, termination, and media issues
- **Monitor** call and media quality, including answer seizure ratio, network effectiveness ratio, post-dial delay, jitter, delay, MOS, and packet loss
- **Monitor** SIP registration activity and SIP recording sessions
- **View** active alarms raised by the device, both as a summary and per individual alarm
- **Control** SIP diagnostic test calls - start a test call from the device and stop it on demand

## Prerequisites for setting up connection to AudioCodes Mediant adapter
The AudioCodes Mediant adapter communicates with the device over HTTP/HTTPS using the device's REST API, authenticated with HTTP Basic authentication. It requires a valid AudioCodes device user account.

Before integrating an AudioCodes Mediant device with Symphony, the following prerequisites must be completed:
- An AudioCodes device user account, configured in the device's Local Users table
- The username and password for that account
- The account must have at least Monitor privilege level; availability of specific REST API endpoints depends on the user's privilege level on the device

The Symphony instance or Cloud Connector must be able to reach:
- Management address: the device host
- Protocol / Port: HTTPs, port 443 (default; confirm against the customer's device setup)

Firewall or proxy rules must allow outbound connectivity between the Symphony Cloud Connector and the device on the configured port.

The adapter supports AudioCodes Session Border Controllers, Media Gateways and MSBR devices that expose the REST API described in the AudioCodes REST API for SBC-Gateway-MSBR Devices guide (Version 7.4), with monitorable properties determined by each device model's capabilities. Validated support: Mediant 3100 SBC.

## AudioCodes Mediant Device Connection Setup

Note: The connection configuration below describes a successful AudioCodes Mediant integration setup. These should not be confused with the adapter configuration parameters. They are not to be inferred as troubleshooting checks and should not be used when diagnosing specific errors unless a troubleshooting entry (provided in the Troubleshooting section) explicitly references them.

Once the device is network-accessible, use the following settings to configure the device in Symphony (available on the Configuration tab of the device configuration):

| Field | Description |
|---|---|
| Device Type | Infrastructure |
| Category | Gateway |
| Manufacturer | Audiocodes |
| Model | Device model, for example Mediant 3100 SBC |
| Monitoring Service | Advanced Monitoring |
| Ping Protocol | ICMP |
| Monitoring Source | Direct |
| Management Address | Device Host |
| Protocol | HTTPs |
| Username | <Username> |
| Password | <Password> |
| Port Number | 443 |

When the device is configured, saved, and set active, Symphony retrieves device status, network information, statistics, and alarms from the device and displays them under Extended Properties. There is no provisioning step - the device is monitored directly.

Adapter behavior can be tuned via the following Adapter Configuration Parameters:

| Property | Description |
| --- | --- |
| displayPropertyGroups | Comma-separated list of optional property groups to display. Defaults to All. Possible values: All (default), Network, ActiveAlarms, CallDiagnostics, CallLoadStatistics, CallQualityStatistics, CallTerminationStatistics, CallMediaIssuesStatistics, CallCapacityStatistics, CallRoutingStatistics, CallTrafficStatistics, MediaStatistics, MediaDSPStatistics, MediaClusterStatistics, RegistrationStatistics, SIPRecStatistics. General and Adapter Metadata properties are never gated and are always shown. |
| historicalProperties | Comma-separated list of properties reported as dynamic statistics, so Symphony stores them as a time series and can graph them. Defaults to none. Give the property name only, without the group prefix. Possible values: AnswerSeizureRatio(%), NetworkEffectivenessRatio(%), FailedCallsInRatio(%), FailedCallsOutRatio(%), PostDialDelay(sec), ActiveSessions, AttemptedCallsRateIn(cps), AttemptedCallsRateOut(cps), MediaPacketLossIn(%), MediaPacketLossOut(%), MediaJitterIn(ms), MediaJitterOut(ms), MediaBandwidthIn(Kbps), MediaBandwidthOut(Kbps), DSPResourceCurrent(%), RegisteredUsers. Example: AnswerSeizureRatio(%),ActiveSessions,MediaJitterIn(ms) |

Note: Both parameters are matched case-sensitively. Unrecognised displayPropertyGroups values are ignored and logged as a warning listing the supported values; if every supplied value is unrecognised, the adapter falls back to All and logs a separate warning.

Note: The adapter does not expose a configurable API polling interval. Data is retrieved on Symphony's standard polling cycle for the device.

## AudioCodes Mediant - Filtering Device(s)

Device filtering is not applicable. This is a single-device adapter with no aggregation, so there is no device set to filter. Property-level filtering is handled entirely by `displayPropertyGroups`.

| Property | Description | Default |
|---|---|---|
| displayPropertyGroups | Comma-separated list of optional property groups to display | All |

## Available Monitored Data for AudioCodes Mediant adapter
The AudioCodes Mediant adapter exposes properties across the following groups. Controllable properties are marked with an asterisk (*).

| Property Group | Description |
|---|---|
| General (ungrouped) | High Availability, Local Timestamp, MC Upgrade Status, Operational State, Product Type, Protocol Type, Reset Needed, Save Needed, Serial Number, System Uptime (sec), Upgrade Status, Version ID. These keys are emitted without a group prefix, so they appear at the top of Extended Properties rather than under a General heading |
| Adapter Metadata | Active Property Groups, Adapter Build Date, Adapter Uptime, Adapter Uptime (min), Adapter Version |
| Network | Default Gateway, IP Address, MAC Address, Subnet Mask |
| Active Alarms | Count, Severity (comma-separated list of the distinct severities across all active alarms), Sources (comma-separated list of the distinct sources) |
| Active Alarms per alarm (ActiveAlarms_[AlarmID]) | Date, Description, Severity, Source. Each active alarm is reported in its own group, named using the device's own identifier for that alarm; those identifiers come from the device and are not sequential |
| Call Diagnostics | Called Number*, Calling Number*, Destination*, Start*, Stop*, Call ID, Release Cause, Status |
| Call Capacity Statistics | Admission Failed Calls In Total, Admission Failed Calls Out Total, No Resources Calls In Total, No Resources Calls Out Total |
| Call Load Statistics | Active Sessions (graphable), Busy Calls In Total, Busy Calls Out Total |
| Call Media Issues Statistics | Media Mismatch Calls In Total, Media Mismatch Calls Out Total |
| Call Quality Statistics | Answer Seizure Ratio (%) (graphable), Failed Calls In Ratio (%) (graphable), Failed Calls Out Ratio (%) (graphable), Network Effectiveness Ratio (%) (graphable), Post Dial Delay (sec) (graphable) |
| Call Routing Statistics | No Route Calls In Total |
| Call Termination Statistics | Abnormal Terminated Calls In Total, Abnormal Terminated Calls Out Total, Media Broken Connection Calls Total |
| Call Traffic Statistics | Attempted Calls Rate In (cps) (graphable), Attempted Calls Rate Out (cps) (graphable), No Answer Calls In Total, No Answer Calls Out Total |
| Media Cluster Statistics | DSP Cluster Utilization (%), Media Cluster Utilization (%) |
| Media DSP Statistics | DSP Resource Current (%) (graphable), SBC Sessions Coder Transcoding, SBC Sessions Coder Transcoding (%) |
| Media Statistics | Media Bandwidth In (Kbps) (graphable), Media Bandwidth Out (Kbps) (graphable), Media Delay In (ms), Media Delay Out (ms), Media Jitter In (ms) (graphable), Media Jitter Out (ms) (graphable), Media MOS In, Media MOS Out, Media Packet Loss In (%) (graphable), Media Packet Loss Out (%) (graphable), Media RTP Streams, Media Streams |
| Registration Statistics | Registered Users (graphable), Register Rate In (rps), Register Rate Out (rps), SBC Registration Success Ratio (%), Transaction Rate (tps), User Registration Success Ratio (%) |
| SIP Rec Statistics | SIP Rec Rate (sps), SIP Rec Sessions |

**Call Diagnostics controls:**

| Control | Description |
|---|---|
| Called Number* | The URI of the called number for the test call, in user@host form. Maximum 61 characters |
| Calling Number* | The URI of the calling number for the test call, in user@host form. Maximum 61 characters |
| Destination* | The destination address the device routes the test call to. Per the AudioCodes REST API this is an IP address or DNS name, optionally with a port - not a SIP URI. Maximum 50 characters |
| Start* | Dials a new test call using the staged Called Number, Calling Number and Destination values |
| Stop* | Drops the active test call. Shown only while a call is active |

**Notes:**
- The General and Network properties are both sourced from the device's status response. If that response is unavailable, neither set is reported for that polling cycle.
- Ratio, average and quality properties are only calculated by the device once calls have passed through it. On an idle device these correctly report N/A, while the plain counters alongside them report 0.
- Property availability varies by Mediant model; branches the device does not support are skipped and reported as N/A.
- Cluster utilisation properties are not meaningful on a non-clustered SBC.

## Troubleshooting checks for AudioCodes Mediant

**Troubleshooting guidance**
- If an error occurs, focus only on troubleshooting steps that are provided in the section below.
- Do not include prerequisite/setup information.
- Do not add unrelated configuration details from other sections.
- If the document does not provide a direct error troubleshooting step, state that the document does not contain enough guidance for that specific issue.

**Login Error**
- Verify the Username and Password correspond to a valid AudioCodes device user account
- Verify the account has not been locked, disabled, or expired on the device
- Verify the account has at least Monitor privilege level on the device

**API Error / Link Error / Connection Error / Ping Timeout**
Note: Connection errors are distinct from login/authentication errors. If credentials are incorrect, refer to the Login Error section instead.
- Verify the Management Address (device host) is correct and reachable from the Symphony Cloud Connector
- Confirm the configured Protocol and Port Number match the device's setup (HTTPs, port 443 by default)
- Verify outbound connectivity and firewall/proxy configuration between the Symphony Cloud Connector and the device

**A Property Group Is Not Displayed**
- Verify the group name is listed in displayPropertyGroups and is spelled exactly as documented; matching is case-sensitive
- Check the adapter log for a warning listing the supported values, which indicates an unrecognised group name was supplied
- Note that General and Adapter Metadata are always shown and are not controlled by displayPropertyGroups

**Properties Display N/A Instead of a Value**
- Ratio, average and quality properties (for example Answer Seizure Ratio, Post Dial Delay, jitter, delay, MOS, packet loss) are only calculated by the device once calls have passed through it; N/A is expected on an idle device
- Verify the device model supports that property; unsupported branches are reported as N/A
- Cluster utilisation properties report N/A on a non-clustered SBC
- If a value is expected, check the adapter log for a message indicating the individual property could not be retrieved or parsed

**A Property Is Not Being Graphed**
- Verify the property name is listed in historicalProperties, without its group prefix, and spelled exactly as documented; matching is case-sensitive
- Verify the property's group is enabled by displayPropertyGroups; a property is only reported as a dynamic statistic if its group is enabled
- Non-numeric values are skipped for that polling cycle and logged, which shows as a gap in the graph rather than a substituted value

**Diagnostic Test Call Fails to Start**
- Verify Called Number, Calling Number and Destination are all set; the call is rejected if any is empty
- Verify none of the three values exceeds its character limit (61, 61 and 50 respectively)
- Verify the Destination value is an IP address or DNS name that the device can route to, optionally with a port - a SIP URI such as user@host is not valid in this field, although it is correct for Called Number and Calling Number
- Verify the destination endpoint is reachable from the device and is accepting SIP calls

If none of the recommended steps help, please enter an SOS ticket at {https://avi-spl.atlassian.net/servicedesk/customer/portals}

## What AI Assistant can do with AudioCodes Mediant integration:
- Find AudioCodes Mediant devices in Symphony (Infrastructure | Gateway | Audiocodes)
- Verify the AudioCodes Mediant adapter configuration and connectivity status

## What AI Assistant cannot do with AudioCodes Mediant integration:
- Modify device-side settings, SIP interfaces, IP Groups, routing rules or alarm configuration - these must be configured directly on the AudioCodes device
- Place production calls; the Call Diagnostics group runs the device's own SIP test call feature only

## AudioCodes Mediant - Additional Resources
For more questions and details about the adapter and its configuration, please refer to:
- AudioCodes Mediant Release Notes: https://avi-spl.atlassian.net/wiki/spaces/SYM/pages/5325127683/AudioCodes+Mediant+Release+Notes

Please make sure to mention this is a direct connection between the Symphony Cloud Connector and a single AudioCodes Mediant device (Management Address). The adapter does not aggregate or discover other devices.
