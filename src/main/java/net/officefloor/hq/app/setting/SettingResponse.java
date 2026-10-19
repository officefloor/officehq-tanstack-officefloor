package net.officefloor.hq.app.setting;

/** A setting as the screen shows it: its key and value ("" when blank). */
public record SettingResponse(String key, String value) {
    public static SettingResponse from(Setting setting) {
        return new SettingResponse(setting.key(), setting.value());
    }
}
