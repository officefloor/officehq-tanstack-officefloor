package net.officefloor.hq.app.setting;

/** The settings form as submitted: the clerk limit (blank: no limit). */
public record SettingsRequest(String clerkLimit) {
}
