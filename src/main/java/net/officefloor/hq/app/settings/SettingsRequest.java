package net.officefloor.hq.app.settings;

/** The settings form: each setting given is saved; one left out (null) stays as it is. The grade terms are by grade. */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        java.util.Map<String, String> gradeTerms, String cap) {
}
