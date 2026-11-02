package net.officefloor.hq.app.settings;

import java.util.List;

/** A sheet's settings: its clerk limit (two decimals; null means no limit), its floor (two decimals:
 *  the least a live due can be), its terms (the days after a claim's date that it is due by), its late percent (two decimals: what an
 *  overdue claim's due gains, of its figure, for every full 30 days it is overdue), the roles set on it, by user name. A name not listed is a clerk. And its policy date (yyyy-MM-dd; null when blank,
 *  meaning the sheet's today): a claim dated before it rounds half up, one on or after it half to even. And its period numbering from-date (yyyy-MM-dd; null when blank, meaning off):
 *  while set and the sheet's today is on or after it, a claim opened is numbered within its period. And each grade's own terms,
 *  by grade (a grade not listed uses the sheet's terms). And its cap (two decimals; null means none): a
 *  person's due-total is at most it. The clerk limit, floor, terms, late percent, grade terms and cap are those in
 *  force on the sheet's today; {@code dated} is every value recorded for each of them, by key (clerk-limit, floor,
 *  terms, terms-<grade>, late-pct, cap, proof-threshold), oldest from-date first. {@code permissions} is the permission
 *  matrix entries set, by action (open, settle, void, reassign, close, reopen), each a comma separated list of roles; an
 *  action not listed keeps the standing rule. */
public record Settings(String clerkLimit, String floor, int terms, String latePct, List<Role> roles, String policyDate,
        String periodNumberingFrom, java.util.Map<String, Integer> gradeTerms, String cap,
        java.util.Map<String, List<DatedValue>> dated, java.util.Map<String, String> permissions) {
}
