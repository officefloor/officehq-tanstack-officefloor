package net.officefloor.hq.app.setting;

/** The settings as the screen shows them: the clerk limit with two decimals ('' when there is none) and the floor
 *  with two decimals and the terms in days and the late percent with two decimals. */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct) {
    public static SettingsResponse from(Settings s) {
        return new SettingsResponse(SettingsResponse.text(s.clerkLimit()), SettingsResponse.text(s.floor()),
                String.valueOf(s.terms()), SettingsResponse.text(s.latePct()));
    }

    static String text(java.math.BigDecimal limit) {
        return limit != null ? limit.toPlainString() : "";
    }
}
