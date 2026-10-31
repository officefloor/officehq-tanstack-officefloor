package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.util.List;

/** The sheet's settings as the screen sees them: the clerk limit with two decimals (null for no
 *  limit), the floor with two decimals, the terms in days, the late percent with two decimals and the roles given, by user name. */
public record SettingsResponse(String clerkLimit, String floor, int terms, String latePct, List<RoleResponse> roles) {
    public static SettingsResponse of(String sheetId, Settings settings) {
        BigDecimal limit = settings.clerkLimit(sheetId);
        return new SettingsResponse(limit != null ? limit.toPlainString() : null,
                settings.floor(sheetId).toPlainString(), settings.terms(sheetId),
                settings.latePct(sheetId).toPlainString(), settings.roles(sheetId));
    }
}
