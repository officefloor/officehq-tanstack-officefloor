package net.officefloor.hq.app.setting;

/** The settings form: the request body to set one setting's value (blank clears it). */
public record SettingRequest(String value) {
}
