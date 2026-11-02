package net.officefloor.hq.app.settings;

/** The settings form: each setting given is saved; one left out (null) stays as it is. */
public record SettingsRequest(String clerkLimit, String floor) {
}
