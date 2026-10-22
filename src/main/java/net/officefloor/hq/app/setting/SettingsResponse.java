package net.officefloor.hq.app.setting;

/** The settings as the screen shows them: the clerk limit with two decimals ('' when there is none). */
public record SettingsResponse(String clerkLimit) {
    public static SettingsResponse from(Settings s) {
        return new SettingsResponse(SettingsResponse.text(s.clerkLimit()));
    }

    static String text(java.math.BigDecimal limit) {
        return limit != null ? limit.toPlainString() : "";
    }
}
