package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;

/** The sheet's settings as the screen shows them: the clerk limit with two decimals (null: no limit) and the
 *  floor with two decimals, and the terms in days, and the late percent with two decimals, and the policy date (YYYY-MM-DD; "" when
 *  blank: the sheet's today), and the period numbering from (YYYY-MM-DD; "" when off). */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate,
        String periodNumberingFrom) {

    public static SettingsResponse from(Sheet sheet) {
        return new SettingsResponse(sheet.getClerkLimit() == null ? null : sheet.getClerkLimit().setScale(2).toPlainString(),
                sheet.getFloor().setScale(2).toPlainString(), String.valueOf(sheet.getTerms()),
                sheet.getLatePct().setScale(2).toPlainString(),
                sheet.getPolicyDate() == null ? "" : sheet.getPolicyDate().toString(),
                sheet.getPeriodNumberingFrom() == null ? "" : sheet.getPeriodNumberingFrom().toString());
    }
}
