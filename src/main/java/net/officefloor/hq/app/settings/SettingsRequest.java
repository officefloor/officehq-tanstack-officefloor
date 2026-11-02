package net.officefloor.hq.app.settings;

/** The settings form: each setting given is saved; one left out (null) stays as it is. The grade terms are by grade.
 *  {@code from} is the from-date given for a dated setting, by key (clerk-limit, floor, terms, terms-<grade>, late-pct,
 *  cap, proof-threshold); blank or left out is the sheet's today. {@code permissions} is the permission matrix entries
 *  given, by action: a comma separated list of roles, blank keeping the standing rule. */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        java.util.Map<String, String> gradeTerms, String cap, java.util.Map<String, String> from,
        String proofThreshold, java.util.Map<String, String> permissions) {
}
