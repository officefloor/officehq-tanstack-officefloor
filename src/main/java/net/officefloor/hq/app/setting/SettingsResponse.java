package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;

/** The sheet's settings as the screen shows them: the clerk limit with two decimals (null: no limit) and the
 *  floor with two decimals, and the terms in days, and the late percent with two decimals, and the policy date (YYYY-MM-DD; "" when
 *  blank: the sheet's today), and the period numbering from (YYYY-MM-DD; "" when off), and the own terms in days of grades A, B and C
 *  ("" when the grade uses the sheet's terms), and the cap with two decimals ("" when there is none). */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate,
        String periodNumberingFrom, String termsA, String termsB, String termsC, String cap) {

    public static SettingsResponse from(Sheet sheet) {
        return new SettingsResponse(sheet.getClerkLimit() == null ? null : sheet.getClerkLimit().setScale(2).toPlainString(),
                sheet.getFloor().setScale(2).toPlainString(), String.valueOf(sheet.getTerms()),
                sheet.getLatePct().setScale(2).toPlainString(),
                sheet.getPolicyDate() == null ? "" : sheet.getPolicyDate().toString(),
                sheet.getPeriodNumberingFrom() == null ? "" : sheet.getPeriodNumberingFrom().toString(),
                gradeTerms(sheet, "A"), gradeTerms(sheet, "B"), gradeTerms(sheet, "C"),
                sheet.getCap() == null ? "" : sheet.getCap().setScale(2).toPlainString());
    }

    /** The grade's own terms in days; "" when it uses the sheet's terms. */
    public static String gradeTerms(Sheet sheet, String grade) {
        Integer terms = sheet.getGradeTerms(grade);
        return terms == null ? "" : String.valueOf(terms);
    }
}
