package net.officefloor.hq.app.setting;

/** A sheet's settings, as the screen reads them: the clerk limit with two decimals ('' for none). */
public record SettingsResponse(String clerkLimit) {
    public static SettingsResponse from(Settings settings) {
        return new SettingsResponse(settings.clerkLimit() == null ? "" : settings.clerkLimit().toPlainString());
    }
}
