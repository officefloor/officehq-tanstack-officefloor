package net.officefloor.hq.app.setting;

/** The body of the settings being saved, as typed: the clerk limit (blank: no limit) and the floor, the terms and the late percent (each null or blank: left as it is), and the policy date (null: left as it is; blank: none, so the sheet's today), and the period numbering from-date (null: left as it is; blank: off), and the terms of a grade's own, by grade (each left out, null or blank: left as it is). */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        java.util.Map<String, String> gradeTerms) {
}
