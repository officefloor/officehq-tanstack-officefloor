package net.officefloor.hq.app.setting;

/**
 * The settings of a sheet being saved, each null when it is left as it is: the clerk limit with two decimals,
 * "" for no limit, the floor with two decimals, the terms as a whole number of days, the late percent with two decimals, and the policy date as yyyy-MM-dd, "" for none, and the period numbering from-date as yyyy-MM-dd, "" for off, and the cap with two decimals, "" for none.
 * {@code gradeTerms} are the terms of a grade's own being saved, as a whole number of days, by grade (a grade left out is left as it is).
 */
public record SheetSettings(String sheetId, String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom, String cap,
        java.util.SortedMap<String, String> gradeTerms) {
}
