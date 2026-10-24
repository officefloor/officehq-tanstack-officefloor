package net.officefloor.hq.app.setting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetSetting;

/** The sheet's settings as the screen shows them, each in force on the sheet's today: the clerk limit with two decimals (null: no limit) and the
 *  floor with two decimals, and the terms in days, and the late percent with two decimals, and the policy date (YYYY-MM-DD; "" when
 *  blank: the sheet's today), and the period numbering from (YYYY-MM-DD; "" when off), and the own terms in days of grades A, B and C
 *  ("" when the grade uses the sheet's terms), and the cap with two decimals ("" when there is none), and the proof threshold with two decimals ("" when there is none); and the values recorded
 *  for each dated setting, by key (floor, late-pct, terms, terms-A/B/C, clerk-limit, cap), by from-date; and the permission matrix
 *  entry of each of open, settle, void, reassign, close and reopen (its comma separated roles; "" when blank: the standing rule). */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate,
        String periodNumberingFrom, String termsA, String termsB, String termsC, String cap, String proofThreshold,
        Map<String, List<Dated>> dated, String permOpen, String permSettle, String permVoid, String permReassign,
        String permClose, String permReopen) {

    /** A value recorded from a date on (null from: the beginning of time; null value: blank). */
    public record Dated(String from, String value) {
    }

    public static SettingsResponse from(Sheet sheet) {
        Map<String, List<Dated>> dated = new LinkedHashMap<>();
        for (SheetSetting s : sheet.getSettings()) {
            dated.computeIfAbsent(s.getKey(), k -> new ArrayList<>())
                    .add(new Dated(s.getFromDate() == null ? null : s.getFromDate().toString(), s.getValue()));
        }
        return new SettingsResponse(sheet.getClerkLimit() == null ? null : sheet.getClerkLimit().toPlainString(),
                sheet.floorOn(sheet.getToday()).setScale(2).toPlainString(), String.valueOf(sheet.termsOn(sheet.getToday())),
                sheet.latePctOn(sheet.getToday()).setScale(2).toPlainString(),
                sheet.getPolicyDate() == null ? "" : sheet.getPolicyDate().toString(),
                sheet.getPeriodNumberingFrom() == null ? "" : sheet.getPeriodNumberingFrom().toString(),
                gradeTerms(sheet, "A"), gradeTerms(sheet, "B"), gradeTerms(sheet, "C"),
                sheet.getCap() == null ? "" : sheet.getCap().toPlainString(),
                sheet.proofThresholdOn(sheet.getToday()) == null ? "" : sheet.proofThresholdOn(sheet.getToday()).toPlainString(),
                dated, sheet.permOf("open"), sheet.permOf("settle"), sheet.permOf("void"), sheet.permOf("reassign"),
                sheet.permOf("close"), sheet.permOf("reopen"));
    }

    /** The grade's own terms in days in force on the sheet's today; "" when it uses the sheet's terms. */
    public static String gradeTerms(Sheet sheet, String grade) {
        Integer terms = sheet.gradeTermsOn(grade, sheet.getToday());
        return terms == null ? "" : String.valueOf(terms);
    }
}
