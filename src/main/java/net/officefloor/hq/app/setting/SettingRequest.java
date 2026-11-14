package net.officefloor.hq.app.setting;

/** Body of changing a setting; a blank value clears it. A dated key's value applies from its
 *  from-date (blank: the sheet's today); an undated key takes no from-date. */
public record SettingRequest(String key, String value, String from) {
}
