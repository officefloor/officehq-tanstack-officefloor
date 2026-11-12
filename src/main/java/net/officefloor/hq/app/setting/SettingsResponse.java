package net.officefloor.hq.app.setting;

import java.util.List;
import java.util.Map;

/** The sheet's settings as the front end reads them: {@code clerkLimit} has two decimals, null for
 *  no limit; {@code floor} has two decimals; {@code terms} is the whole days after its date a claim is
 *  due by;
 *  {@code latePct} has two decimals; {@code policyDate} is the date rounding turns on, null when blank
 *  (then the sheet's today); {@code periodNumberingFrom} is the date from which claims opened are
 *  numbered per period, null when blank (off); {@code gradeTerms} is the terms in force for each grade
 *  (its own, else the sheet's), by grade; {@code cap} has two decimals, the most a person's due-total may
 *  be, null for no cap; {@code proofThreshold} has two decimals, the due above which a claim needs a proof
 *  before it is settled or approved, null for none; {@code noteThreshold} has two decimals, the due above
 *  which a claim needs a note before it is opened, null for none. Each of these that carries a figure is the one in force on the sheet's today.
 *  {@code floorFrom} and {@code latePctFrom} are the floor and late percent recorded, oldest from-date
 *  first (a claim's floor is the one in force on its date, its late percent the one in force on each 30-day boundary). {@code perms} is the permission
 *  matrix: the roles that may take each action (open, settle, void, reassign, close, reopen), comma
 *  separated, by action; null when blank (the standing rule).
 *  {@code graceDays} is the whole days after its to-date an open period is closed by a new today, null when blank (off).
 *  {@code entryDate} is the default date of a new claim in place of the sheet's today, null when blank (off).
 *  {@code gradeGrace} is each grade's grace days in force on the sheet's today, by grade, null when it has none;
 *  {@code gradeGraceFrom} is each grade's grace days recorded, by grade, oldest from-date first (a claim's grace is the
 *  one in force on its date for its person's grade then).
 *  {@code gradeProofThreshold} is each grade's proof threshold in force on the sheet's today, by grade, null when it has
 *  none (a claim's proof threshold is its person's grade's in force on its date when one is set, else the sheet's).
 *  {@code pot} has two decimals, the sum shared out among a balanced pool's members, null for none (not dated). */
public record SettingsResponse(String clerkLimit, String floor, String terms, String latePct, String policyDate,
        String periodNumberingFrom, Map<String, String> gradeTerms, String cap, String proofThreshold, List<Dated> floorFrom,
        List<Dated> latePctFrom, Map<String, String> perms, String graceDays,
        String noteThreshold, String entryDate, Map<String, String> gradeGrace,
        Map<String, List<Dated>> gradeGraceFrom, Map<String, String> gradeProofThreshold,
        String pot) {

    /** A value recorded from a date (yyyy-MM-dd). */
    public record Dated(String from, String value) {
    }
}
