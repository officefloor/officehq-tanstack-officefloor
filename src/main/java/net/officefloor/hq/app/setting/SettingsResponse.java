package net.officefloor.hq.app.setting;

/**
 * A sheet's settings, as the screen reads them: the clerk limit with two decimals ('' for none) and
 * the floor with two decimals.
 */
public record SettingsResponse(String clerkLimit, String floor) {
    public static SettingsResponse from(Settings settings) {
        return new SettingsResponse(settings.clerkLimit() == null ? "" : settings.clerkLimit().toPlainString(),
                settings.floor().setScale(2).toPlainString());
    }
}
