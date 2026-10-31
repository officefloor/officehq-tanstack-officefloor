package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** The sheet's settings as the screen sees them, each dated one as in force on the sheet's today:
 *  the clerk limit with two decimals (null for no limit), the floor with two decimals, the terms in days, the late percent with two decimals, the policy date as set (yyyy-mm-dd, null when blank), the period numbering from (yyyy-mm-dd, null when off), the terms set per grade, by grade (a grade not in it uses the sheet's terms), the cap with two decimals (null for no cap), and the roles given, by user name;
 *  and every value recorded for the dated settings, by key, oldest from-date first; and the proof threshold
 *  with two decimals (null for none); and the permissions set, by action (roles comma separated; an action not in it keeps the standing rule); and the grace days (null when off); and every delegation of the steward role, oldest first. */
public record SettingsResponse(String clerkLimit, String floor, int terms, String latePct, String policyDate, String periodNumberingFrom, Map<String, Integer> gradeTerms, String cap, List<RoleResponse> roles,
        Map<String, List<Settings.DatedValue>> dated, String proofThreshold, Map<String, String> perms, Integer graceDays,
        List<DelegationResponse> delegations) {
    public static SettingsResponse of(String sheetId, LocalDate today, Settings settings) {
        BigDecimal limit = settings.clerkLimit(sheetId);
        LocalDate policyDate = settings.policyDate(sheetId);
        BigDecimal cap = settings.cap(sheetId);
        BigDecimal threshold = settings.proofThreshold(sheetId, today);
        LocalDate numberingFrom = settings.periodNumberingFrom(sheetId);
        return new SettingsResponse(limit != null ? limit.toPlainString() : null,
                settings.floor(sheetId, today).toPlainString(), settings.terms(sheetId, today),
                settings.latePct(sheetId, today).toPlainString(),
                policyDate != null ? policyDate.toString() : null,
                numberingFrom != null ? numberingFrom.toString() : null, settings.gradeTerms(sheetId),
                cap != null ? cap.toPlainString() : null, settings.roles(sheetId), settings.dated(sheetId),
                threshold != null ? threshold.toPlainString() : null, settings.permissions(sheetId),
                settings.graceDays(sheetId), settings.delegations(sheetId));
    }
}
