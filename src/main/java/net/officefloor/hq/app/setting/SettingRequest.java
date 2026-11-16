package net.officefloor.hq.app.setting;

/** Body of setting a sheet setting: the value as text (blank for none), the from-date (yyyy-MM-dd; blank: the
 *  sheet's today; ignored by an undated key), and who sets it. */
public record SettingRequest(String value, String from, String by) {
}
