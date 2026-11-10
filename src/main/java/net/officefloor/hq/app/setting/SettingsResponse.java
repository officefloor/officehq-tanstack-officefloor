package net.officefloor.hq.app.setting;

/**
 * A sheet's settings, as the screen reads them: the clerk limit with two decimals ('' for none), the
 * floor with two decimals and the terms in days.
 */
public record SettingsResponse(String clerkLimit, String floor, String terms) {
    public static SettingsResponse from(Settings settings) {
        return new SettingsResponse(settings.clerkLimit() == null ? "" : settings.clerkLimit().toPlainString(),
                settings.floor().setScale(2).toPlainString(), Integer.toString(settings.terms()));
    }
}
