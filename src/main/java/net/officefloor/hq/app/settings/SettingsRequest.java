package net.officefloor.hq.app.settings;

/** The settings form: each setting given is saved; one left out (null) stays as it is. The grade terms are by grade.
 *  {@code from} is the from-date given for a dated setting, by key (clerk-limit, floor, terms, terms-<grade>, late-pct,
 *  cap, proof-threshold, note-threshold); blank or left out is the sheet's today. {@code permissions} is the permission matrix entries
 *  given, by action: a comma separated list of roles, blank keeping the standing rule. {@code graceDays} is the grace days, blank meaning off.
 *  {@code entryDate} is the entry date, blank meaning off. {@code gradeGrace} is each grade's grace days given, by grade
 *  (dated, from-date key grace-<grade>), blank meaning none. {@code gradeProofThreshold} is each grade's proof threshold
 *  given, by grade (dated, from-date key proof-threshold-<grade>), blank meaning none (the sheet's is then used).
 *  {@code pot} is the pot, blank meaning none (not dated). {@code parent} is the parent sheet's id, blank meaning none
 *  (not dated). {@code fiscalStartMonth} is the fiscal year start month, 1 to 12 (not dated). {@code alertThreshold} is the alert threshold, blank meaning off (not dated). */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        java.util.Map<String, String> gradeTerms, String cap, java.util.Map<String, String> from,
        String proofThreshold, java.util.Map<String, String> permissions, String graceDays,
        String noteThreshold, String entryDate, java.util.Map<String, String> gradeGrace,
        java.util.Map<String, String> gradeProofThreshold, String pot, String parent,
        String fiscalStartMonth, String alertThreshold) {
}
