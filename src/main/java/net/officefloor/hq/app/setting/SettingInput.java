package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/**
 * A validated setting: a known key and its value as stored ("" for the default), and the from-date given for a dated
 * key (null when none was: the sheet's today).
 */
public record SettingInput(String key, String value, LocalDate from) {

    public SettingInput(String key, String value) {
        this(key, value, null);
    }

    /** This setting from the from-date. */
    public SettingInput withFrom(LocalDate from) {
        return new SettingInput(key, value, from);
    }
}
