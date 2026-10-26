package net.officefloor.hq.app.setting;

/**
 * The settings of a sheet being saved, each null when it is left as it is: the clerk limit with two decimals,
 * "" for no limit, the floor with two decimals, the terms as a whole number of days, the late percent with two decimals, and the policy date as yyyy-MM-dd, "" for none, and the period numbering from-date as yyyy-MM-dd, "" for off, and the cap and the proof threshold, each with two decimals, "" for none.
 * {@code gradeTerms} are the terms of a grade's own being saved, as a whole number of days, by grade (a grade left out is left as it is).
 * {@code from} is the from-date given for a dated setting, by setting key (a key left out is saved from the sheet's today, and audited without one).
 */
public record SheetSettings(String sheetId, String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom, String cap, String proofThreshold,
        java.util.SortedMap<String, String> gradeTerms, java.util.Map<String, java.time.LocalDate> from) {
}
