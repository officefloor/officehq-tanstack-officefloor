package net.officefloor.hq.app.setting;

/** Body of a settings save: who is making it and the clerk limit (blank: no limit). */
public record SettingsRequest(String user, String clerkLimit) {
}
