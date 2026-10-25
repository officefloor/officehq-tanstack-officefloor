package net.officefloor.hq.app.access;

import java.util.List;

/** A setting: its value in force on the sheet's today (null: blank) and, for a dated one, every value recorded by from-date. */
public record SettingResponse(String key, String value, List<From> dated) {

    /** A dated setting's value from a date. */
    public record From(String from, String value) {
    }

    public static SettingResponse from(Setting setting) {
        return new SettingResponse(setting.key(), setting.value(), List.of());
    }
}
