package net.officefloor.hq.app.access;

/** A validated setting: its key and its normalised value (null: blank). */
public record SettingInput(String key, String value) {
}
