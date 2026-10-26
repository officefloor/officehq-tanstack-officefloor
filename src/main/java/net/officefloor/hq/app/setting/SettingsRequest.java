package net.officefloor.hq.app.setting;

/** The body of the settings being saved: the clerk limit as typed (blank: no limit). */
public record SettingsRequest(String clerkLimit) {
}
