# Project Blueprint: MQTT Monitor Android App

**Android package ID:** `org.archuser.mqttnotify`

## Core Philosophy

This application is an MQTT-first monitoring and notification terminal designed for technically literate users who operate their own brokers. It makes no claims about guaranteed delivery under Android background constraints. Instead, it offers explicit, user-consented operating modes with clearly defined behavior and tradeoffs.

The app prioritizes honesty, observability, and user control over convenience or automation.

---

## Operating Modes

The application supports **two distinct connection modes**. These modes are explicit, user-selectable, and mutually exclusive.

### Mode 1: Active While Visible (Default)

**Status:** Default, recommended

- MQTT connections are established only while the app UI is visible on screen
- No background execution, alarms, or keepalive attempts
- Screen off or app backgrounding disconnects cleanly
- Notifications may be generated only while connected

**Guarantees:**
- While visible, no subscribed messages are missed
- No false claims of background reliability

**Tradeoffs:**
- No delivery when the app is not actively in use
- Intended for debugging, monitoring, and active observation sessions

This mode treats *UI visibility as consent* for continuous activity.

---

### Mode 2: Persistent Foreground Service (Optional)

**Status:** Optional, off by default

- MQTT connection is maintained via a foreground service
- Requires a persistent notification to remain active
- Operates while the screen is off or app is backgrounded
- Explicit user opt-in is required

**Persistent Notification Requirements:**
The notification must reflect real ongoing work and expose meaningful state, including at minimum:
- Broker name or label
- Connection status
- Elapsed connected time
- Message count since connection

**Guarantees:**
- While the persistent notification is active, the app will not intentionally disconnect
- Message delivery is best-effort, subject to network and broker behavior

**Tradeoffs:**
- Increased battery usage
- Permanent notification occupancy
- User responsibility to disable when not needed

This mode treats *notification presence as consent* for continuous activity.

---

## Notifications

Notifications are a user-facing alert layer and are not required for message ingestion.

- Per-topic notification enablement
- Global temporary mute available via bell menu
- Muting affects notifications only, not message storage

Notifications may be generated in either operating mode, but only while an active connection exists.

---

## Topic Handling

- Topics are treated as independent event streams
- Subscriptions persist across sessions
- Retained messages are flagged as retained
- Retained messages do not count as new activity unless explicitly enabled

Unread counts reflect message arrival, not read state or acknowledgments.

---

## Retention and Storage

- Messages are stored locally per topic
- Retention policies are configurable
- Retention configuration exists but is out of scope for v1 implementation details

---

## Broker Configuration

- Brokers are defined independently of UI labels
- TLS and authentication are supported
- Broker configurations must pass a connection test before being saved

This restriction is intentional and prevents invalid or unreachable configurations from being persisted.

---

## Non-Goals

The application intentionally does **not** provide:

- Guaranteed background delivery
- Task automation or rule engines
- Publishing macros or scripting
- Smart-home dashboards
- Cloud-mediated push notifications

---

## Target Audience

- Self-hosters and homelab operators
- Developers and infrastructure engineers
- IoT and embedded system developers
- Privacy-conscious Android users, including GrapheneOS users

---

## Summary

This application is a monitoring terminal, not a messaging platform. It exposes Android’s execution model clearly and gives users explicit control over how and when continuous connections are maintained. No operating mode hides tradeoffs or promises behavior the platform cannot guarantee.

