# RuneLite cross-plugin configuration integration guide

## Table of contents

- [Purpose](#purpose)
- [Keep plugins decoupled](#keep-plugins-decoupled)
- [Reading another plugin's setting](#reading-another-plugins-setting)
- [Writing another plugin's setting](#writing-another-plugins-setting)
- [One-way synchronization](#one-way-synchronization)
- [Two-way synchronization](#two-way-synchronization)
- [Temporary overrides](#temporary-overrides)
- [Provider-defined integration APIs](#provider-defined-integration-apis)
- [Inherited defaults with localized overrides](#inherited-defaults-with-localized-overrides)
  - [RuneLite notification example](#runelite-notification-example)
  - [Generic provider design](#generic-provider-design)
  - [Scope and ownership](#scope-and-ownership)
  - [What a consumer can and cannot do](#what-a-consumer-can-and-cannot-do)
- [Profiles and persistence](#profiles-and-persistence)
- [Compatibility and failure behavior](#compatibility-and-failure-behavior)
- [Implementation checklist](#implementation-checklist)

## Purpose

This guide explains how one RuneLite plugin can reference or update configuration owned by another plugin without importing that plugin's classes. It is intentionally independent of any particular plugin or feature.

RuneLite stores configuration values under a pair of identifiers:

```text
config group + config key = configuration value
```

For example, a provider plugin might define:

```java
@ConfigGroup("provider-plugin")
public interface ProviderConfig extends Config
{
	@ConfigItem(
		keyName = "exampleSetting",
		name = "Example setting",
		description = "An example provider setting"
	)
	default int exampleSetting()
	{
		return 5;
	}
}
```

Another plugin can access the persisted value through RuneLite's `ConfigManager` using group `"provider-plugin"` and key `"exampleSetting"`.

## Keep plugins decoupled

The consuming plugin should inject RuneLite's `ConfigManager`:

```java
@Inject
private ConfigManager configManager;
```

Do not:

- add the provider plugin as a Gradle dependency;
- import or inject the provider's config interface;
- use reflection to access the provider;
- edit RuneLite configuration files directly.

Referencing the stable group and key strings keeps both plugins independently installable. If the provider is not installed, reading may return no stored value and writing still creates a persisted value for that group/key. The provider will see it if installed later.

Define identifiers once rather than scattering string literals:

```java
private static final String PROVIDER_CONFIG_GROUP = "provider-plugin";
private static final String PROVIDER_SETTING_KEY = "exampleSetting";
```

The group and key are an informal integration contract. Verify them from the provider's current source and treat changes upstream as a compatibility risk.

## Reading another plugin's setting

Read the raw persisted representation with:

```java
String value = configManager.getConfiguration(
	PROVIDER_CONFIG_GROUP,
	PROVIDER_SETTING_KEY);
```

The result can be `null`. A null result usually means no explicit value is stored; it does not necessarily mean the provider's effective setting has no value. Config interfaces commonly supply a default that is returned by their proxy even when nothing is persisted.

The consuming plugin cannot reliably discover that Java default without depending on the provider. Choose and document one of these policies:

1. Treat `null` as a known fallback agreed upon by both plugins.
2. Take no action until the provider has a stored value.
3. Maintain a compatible fallback locally and update it if the provider changes its default.

Parse defensively:

```java
private int readProviderSetting(int fallback)
{
	String value = configManager.getConfiguration(
		PROVIDER_CONFIG_GROUP,
		PROVIDER_SETTING_KEY);

	if (value == null)
	{
		return fallback;
	}

	try
	{
		return Integer.parseInt(value);
	}
	catch (NumberFormatException ex)
	{
		return fallback;
	}
}
```

Use the appropriate parser for booleans, integers, enums, colors, or other supported config representations. Validate ranges after parsing; never assume another plugin's stored value is valid for your use case.

## Writing another plugin's setting

Write through `ConfigManager`:

```java
configManager.setConfiguration(
	PROVIDER_CONFIG_GROUP,
	PROVIDER_SETTING_KEY,
	newValue);
```

This updates the provider's global persisted setting in the active RuneLite profile. It is not scoped to one request or one caller. The provider, its UI, manual usage, and other integrations will observe the same value.

Only do this when global shared ownership is intentional and clear to the user. A consuming plugin should expose descriptive text such as “Updates Provider Plugin's global setting” rather than implying that the value affects only the consuming feature.

Do not unset the provider setting during ordinary cleanup if the intended behavior is persistent sharing:

```java
configManager.unsetConfiguration(
	PROVIDER_CONFIG_GROUP,
	PROVIDER_SETTING_KEY);
```

Unsetting removes the explicit value and causes the provider to fall back to its own default. It does not restore an arbitrary previous value.

## One-way synchronization

The simplest ownership model is one-way synchronization:

```text
consumer setting -> provider setting
```

Subscribe to changes in the consumer's config and write the corresponding provider value:

```java
@Subscribe
public void onConfigChanged(ConfigChanged event)
{
	if (!ConsumerConfig.GROUP.equals(event.getGroup()))
	{
		return;
	}

	if ("sharedSetting".equals(event.getKey()))
	{
		configManager.setConfiguration(
			PROVIDER_CONFIG_GROUP,
			PROVIDER_SETTING_KEY,
			consumerConfig.sharedSetting());
	}
}
```

Filtering by both group and key prevents unrelated changes from triggering writes and prevents the provider change from recursively entering the consumer branch.

Decide explicitly when synchronization occurs:

- **On consumer edit:** the provider changes immediately when the user edits the consumer setting.
- **On feature use:** write immediately before invoking the provider feature.
- **On startup:** the consumer asserts ownership whenever it starts.
- **Combination:** edit plus feature use is often resilient without overwriting the provider merely because the consumer was enabled.

Startup synchronization is the most aggressive choice because it overwrites provider-side edits on every consumer restart.

## Two-way synchronization

Two-way synchronization means changes in either plugin are copied to the other:

```text
consumer setting <-> provider setting
```

Use this only when both UI controls are meant to represent exactly the same preference. It requires an update guard to avoid event loops:

```java
private boolean synchronizingConfig;

@Subscribe
public void onConfigChanged(ConfigChanged event)
{
	if (synchronizingConfig)
	{
		return;
	}

	try
	{
		synchronizingConfig = true;

		if (ConsumerConfig.GROUP.equals(event.getGroup())
			&& "sharedSetting".equals(event.getKey()))
		{
			configManager.setConfiguration(
				PROVIDER_CONFIG_GROUP,
				PROVIDER_SETTING_KEY,
				consumerConfig.sharedSetting());
		}
		else if (PROVIDER_CONFIG_GROUP.equals(event.getGroup())
			&& PROVIDER_SETTING_KEY.equals(event.getKey()))
		{
			String providerValue = configManager.getConfiguration(
				PROVIDER_CONFIG_GROUP,
				PROVIDER_SETTING_KEY);

			if (providerValue != null)
			{
				configManager.setConfiguration(
					ConsumerConfig.GROUP,
					"sharedSetting",
					providerValue);
			}
		}
	}
	finally
	{
		synchronizingConfig = false;
	}
}
```

Before writing, compare normalized current and desired values to avoid redundant `ConfigChanged` events. Establish a conflict policy for startup: provider wins, consumer wins, or synchronize only after the next explicit user edit.

One-way synchronization is preferable unless two-way mirroring is a clear product requirement.

## Temporary overrides

`ConfigManager` does not provide caller-scoped overrides. A temporary override must be simulated by:

1. reading and saving the existing raw value;
2. writing the temporary value;
3. restoring or unsetting it later.

This approach is global while active, can overwrite concurrent user changes, and may fail to restore after an abnormal client termination. Prefer a provider-defined request API, such as a `PluginMessage` containing per-request options, when available.

A config write should not be described as request-local merely because it is later restored.

## Provider-defined integration APIs

Configuration access and plugin messaging solve different problems:

- Use `ConfigManager` when intentionally sharing a persisted global preference.
- Use `PluginMessage` or another provider-defined event contract for commands and per-request options.

Never assume a provider supports a per-request option simply because its message accepts a generic map. Confirm that the provider actually reads and applies the particular key in the relevant code path.

If the provider lacks the required request-local behavior, the cleanest long-term solution is an upstream provider change that explicitly supports it.

## Inherited defaults with localized overrides

RuneLite's notification system demonstrates a stronger integration pattern than cross-plugin `ConfigManager` writes. A consuming plugin owns a local configuration object that can either inherit the provider's defaults or contain a complete local override. The provider receives that object when its service is invoked and decides which values to apply.

Conceptually:

```text
provider global defaults
          |
          v
consumer-local policy ----> provider service call
       inherit/override       resolves effective values
```

This is not one plugin directly overriding another plugin's stored settings. The provider explicitly supports a local policy object.

### RuneLite notification example

A plugin declares a `Notification` value in its own config:

```java
@ConfigItem(
	keyName = "eventNotification",
	name = "Event notification",
	description = "Configures the event notification"
)
default Notification eventNotification()
{
	return Notification.ON;
}
```

When the event occurs, the plugin passes its local value to the shared notification service:

```java
notifier.notify(config.eventNotification(), "An event occurred");
```

The `Notification` object contains both behavior values and metadata such as `enabled`, `initialized`, and `override`. `Notifier` resolves it as follows:

- If the local notification is disabled, do nothing.
- If local override is off, use the general RuneLite notification settings.
- If local override is on and initialized, use the values stored in this plugin's `Notification` config item.

RuneLite's config UI initializes a local override by copying the current general notification settings into the plugin-local `Notification` object. The user can then customize that copy without changing the general settings or another plugin's notification behavior.

The provider owns all important parts of this mechanism:

- the policy value type;
- serialization of that value type;
- UI support for editing inherit/override behavior;
- merging or fallback rules;
- the service method that accepts the local policy.

### Generic provider design

A provider that wants to offer this pattern should publish a stable policy/request type:

```java
public final class FeatureOptions
{
	private final boolean overrideDefaults;
	private final int finishDistance;
	private final boolean showOverlay;

	// Constructor and accessors omitted.
}
```

Its service or message handler must resolve effective settings explicitly:

```java
int effectiveFinishDistance = options != null
	&& options.isOverrideDefaults()
	? options.getFinishDistance()
	: providerConfig.finishDistance();
```

For independently installed Plugin Hub plugins, the same idea can be represented as a documented `PluginMessage` payload rather than a shared Java class:

```java
Map<String, Object> options = new HashMap<>();
options.put("overrideDefaults", true);
options.put("finishDistance", 1);

Map<String, Object> request = new HashMap<>();
request.put("target", target);
request.put("options", options);

eventBus.post(new PluginMessage("provider", "start", request));
```

The provider must actually read, validate, and use each option. A generic map alone does not create override behavior.

### Scope and ownership

The provider must define how long a localized override lives:

- one service call;
- one active operation or route;
- until an explicit clear message;
- for all calls from a named consumer.

Per-operation scope is usually safest. If only one global provider operation can be active at a time, a localized override may still affect whichever operation currently owns that provider. An operation or request ID can make ownership and cleanup explicit.

The provider should clear operation-scoped options with the operation itself and fall back to its global config afterward. It should not persist localized options into its global config group.

### What a consumer can and cannot do

A consumer can use localized overrides only if the provider exposes a suitable integration point. The consumer cannot independently recreate the notification pattern through `ConfigManager`, because `ConfigManager` only reads and writes persisted global values.

If a provider currently reads its config directly inside the active operation, such as:

```java
providerConfig.finishDistance()
```

then a consumer-provided option will have no effect unless the provider changes that code path to resolve the effective value from the local request policy first.

This leads to three distinct integration capabilities:

| Capability | Provider support required | Mutates provider global config |
|---|---:|---:|
| Read another plugin's persisted setting | No | No |
| Write another plugin's persisted setting | No | Yes |
| Inherit defaults with a localized override | Yes | No |

Use the third pattern when the provider supports it. It preserves user defaults, avoids restoration problems, and makes override ownership part of the provider's API instead of an accident of shared configuration.

## Profiles and persistence

Cross-plugin config writes apply to the active RuneLite configuration/profile. They can persist across restarts and may participate in profile synchronization. This is expected for global sharing but must be considered when selecting defaults and ownership behavior.

Do not cache a configuration value indefinitely if the user can change profiles or edit the provider setting. Read it at the point of use or respond to `ConfigChanged` events.

## Compatibility and failure behavior

Design the consumer so the provider remains optional:

- Missing raw configuration must have defined behavior.
- A config write must not be used as proof that the provider is installed or enabled.
- Failure to integrate must not break the consumer's primary feature.
- Avoid logging expected absence at INFO level; use `log.debug()` for diagnostics.
- Do not poll configuration every frame or scan the scene to detect another plugin.

There is no general installation-state guarantee inherent in `ConfigManager`. If installation or readiness matters, use a provider-supported handshake or make the integration best-effort.

## Implementation checklist

- Verify the provider's exact config group, key, type, valid range, and effective default.
- Define group/key constants in the consumer.
- Decide whether the integration reads, writes, synchronizes one-way, or synchronizes two-way.
- Document which plugin owns conflicts.
- Handle `null`, invalid, and out-of-range values.
- Filter `ConfigChanged` by group and key.
- Prevent recursive or redundant writes.
- State clearly whether writes are global and persistent.
- Keep the provider optional; do not add a direct plugin dependency unless its published API explicitly requires one.
- Test with the provider installed, absent, disabled, and edited while the consumer is running.
- Test active-profile changes and client restart persistence.
