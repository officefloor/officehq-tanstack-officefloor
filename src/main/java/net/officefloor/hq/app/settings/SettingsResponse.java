package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** The sheet's settings as the screen sees them: the clerk limit with two decimals (null for no
 *  limit), the floor with two decimals, the terms in days, the late percent with two decimals, the policy date as set (yyyy-mm-dd, null when blank), the period numbering from (yyyy-mm-dd, null when off), the terms set per grade, by grade (a grade not in it uses the sheet's terms), and the roles given, by user name. */
public record SettingsResponse(String clerkLimit, String floor, int terms, String latePct, String policyDate, String periodNumberingFrom, Map<String, Integer> gradeTerms, List<RoleResponse> roles) {
    public static SettingsResponse of(String sheetId, Settings settings) {
        BigDecimal limit = settings.clerkLimit(sheetId);
        LocalDate policyDate = settings.policyDate(sheetId);
        LocalDate numberingFrom = settings.periodNumberingFrom(sheetId);
        return new SettingsResponse(limit != null ? limit.toPlainString() : null,
                settings.floor(sheetId).toPlainString(), settings.terms(sheetId),
                settings.latePct(sheetId).toPlainString(),
                policyDate != null ? policyDate.toString() : null,
                numberingFrom != null ? numberingFrom.toString() : null, settings.gradeTerms(sheetId), settings.roles(sheetId));
    }
}
