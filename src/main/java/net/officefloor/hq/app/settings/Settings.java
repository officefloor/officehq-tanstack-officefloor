package net.officefloor.hq.app.settings;

import java.util.List;

/** A sheet's settings: its clerk limit (two decimals; null means no limit), its floor (two decimals:
 *  the least a live due can be), its terms (the business days after a claim's date that it is due by), its late percent (two decimals: what an
 *  overdue claim's due gains, of its figure, for every full 30 days it is overdue), the roles set on it, by user name. A name not listed is a clerk. And its policy date (yyyy-MM-dd; null when blank,
 *  meaning the sheet's today): a claim dated before it rounds half up, one on or after it half to even. And its period numbering from-date (yyyy-MM-dd; null when blank, meaning off):
 *  while set and the sheet's today is on or after it, a claim opened is numbered within its period. And each grade's own terms,
 *  by grade (a grade not listed uses the sheet's terms). And its cap (two decimals; null means none): a
 *  person's due-total is at most it. The clerk limit, floor, terms, late percent, grade terms and cap are those in
 *  force on the sheet's today; {@code dated} is every value recorded for each of them, by key (clerk-limit, floor,
 *  terms, terms-<grade>, late-pct, cap, proof-threshold, proof-threshold-<grade>), oldest from-date first. {@code permissions} is the permission
 *  matrix entries set, by action (open, settle, void, reassign, close, reopen), each a comma separated list of roles; an
 *  action not listed keeps the standing rule. {@code graceDays} is the days after a period's to-date it is left
 *  open (null when blank: off; not dated): changing the sheet's today closes each open period lapsed past them.
 *  {@code delegations} is each delegation of the steward role made on the sheet, oldest first.
 *  {@code entryDate} is the sheet's entry date (yyyy-MM-dd; null when blank: off; not dated): while set, it is the
 *  default date of a new claim instead of the sheet's today. {@code holidays} is the sheet's holidays (yyyy-MM-dd),
 *  earliest first: a claim's due-by, fixed when it is created, that falls on one is moved to the next day that is not one.
 *  {@code voidReasons} is the sheet's void reasons, in the order they were added: while there is one, voiding a claim
 *  needs one of them. {@code pot} is the sheet's pot (two decimals; null when blank: none; not dated): each member of
 *  a balanced pool gets a cut of it. {@code parent} is the sheet's parent sheet id (null when blank: none; not dated):
 *  a grade with no rate in force on a date takes the parent's, then its parent's, up to three levels up.
 *  {@code fiscalStartMonth} is the month (1 to 12; 1 unless set; not dated) the sheet's fiscal year starts in: a fiscal
 *  year is named FY<yyyy> by the calendar year its first day falls in.
 *  {@code alertThreshold} is the sheet's alert threshold (two decimals; null when blank: off; not dated): a person
 *  whose due-total goes from at or below it to above it is written an ALERT line.
 *  {@code escalateAfter} is the sheet's escalation gap in days (null when blank: off; not dated): reminding a claim
 *  whose previous reminder was at least that many days before the sheet's today escalates it, once per claim ever.
 *  {@code reviewSla} is the sheet's review SLA in days (null when blank: none; not dated): a REVIEW claim whose review
 *  day plus that many days is before the sheet's today is review-late.
 *  {@code pendingSla} is the sheet's pending SLA in days (null when blank: none; not dated): a PENDING claim whose
 *  pending day plus that many days is before the sheet's today is pending-late.
 *  {@code proofKinds} is the sheet's proof kinds (a comma separated list of receipt and photo; null when blank: none;
 *  not dated): a claim above the proof threshold needs a proof of each to be settled or approved.
 *  {@code defaultGrade} is the sheet's default grade (one of its grades; null when blank: A; not dated): the grade the
 *  quick-add bar gives a new person, and the grade of an imported line that leaves it blank.
 *  {@code scoreMid} and {@code scoreHigh} are the sheet's score thresholds (whole numbers; null when blank; not dated):
 *  while both are set, each person shows a band: HIGH at or above the high one, MID at or above the mid one, else LOW.
 *  {@code scoreLog} is the sheet's score log switch (not dated): while on, changing the sheet's today writes one SCORE
 *  line per person with their score as it stood on the day being left.
 *  {@code maxOpen} is the sheet's maximum of open claims per person (null when blank: none; not dated): opening a
 *  claim is refused while its person already has that many OPEN or PENDING claims.
 *  {@code retiredGrades} is the sheet's retired grades (not dated): one cannot be chosen for a person any more (people
 *  already on it keep it), nor be the grade the default grade gives (A is given instead).
 *  {@code grades} is the sheet's grades (not dated): A, B and C, then those added to it in the order added. */
public record Settings(String clerkLimit, String floor, int terms, String latePct, List<Role> roles, String policyDate,
        String periodNumberingFrom, java.util.Map<String, Integer> gradeTerms, String cap,
        java.util.Map<String, List<DatedValue>> dated, java.util.Map<String, String> permissions, Integer graceDays,
        List<Delegation> delegations, String entryDate, List<String> holidays, List<String> voidReasons,
        String pot, String parent, int fiscalStartMonth, String alertThreshold,
        Integer escalateAfter, Integer reviewSla, String proofKinds, String defaultGrade,
        Integer scoreMid, Integer scoreHigh, boolean scoreLog, Integer maxOpen,
        List<String> retiredGrades, List<String> grades, Integer pendingSla) {
}
