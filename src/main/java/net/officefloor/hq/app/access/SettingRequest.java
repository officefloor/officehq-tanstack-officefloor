package net.officefloor.hq.app.access;

/** A setting's key, its value (blank to clear it), the date it is in force from (blank: the sheet's today; dated settings only), and the signed-in user (`by`) making the change. */
public record SettingRequest(String key, String value, String from, String by) {
}
