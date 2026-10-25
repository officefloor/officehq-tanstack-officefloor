package net.officefloor.hq.app.access;

/** A validated setting: its key, its normalised value (null: blank), and the from-date given for a dated one (null: the sheet's today). */
public record SettingInput(String key, String value, java.time.LocalDate from) {
}
