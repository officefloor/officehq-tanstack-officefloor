package net.officefloor.hq.app.access;

public record SettingResponse(String key, String value) {
    public static SettingResponse from(Setting setting) {
        return new SettingResponse(setting.key(), setting.value());
    }
}
