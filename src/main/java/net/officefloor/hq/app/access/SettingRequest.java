package net.officefloor.hq.app.access;

/** A setting's key, its value (blank to clear it), and the signed-in user (`by`) making the change. */
public record SettingRequest(String key, String value, String by) {
}
