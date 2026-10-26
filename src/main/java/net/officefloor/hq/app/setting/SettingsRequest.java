package net.officefloor.hq.app.setting;

/** The body of the settings being saved, as typed: the clerk limit (blank: no limit) and the floor, the terms and the late percent (each null or blank: left as it is), and the policy date (null: left as it is; blank: none, so the sheet's today), and the period numbering from-date (null: left as it is; blank: off), and the cap and the proof threshold (each null: left as it is; blank: none), and the grace days (null: left as it is; blank: off), and the terms of a grade's own, by grade (each left out, null or blank: left as it is), and the from-date (yyyy-mm-dd) each dated setting saved is recorded from, by setting key (left out or blank: the sheet's today), and the roles each action is permitted to, by action, as a comma separated list (left out or null: left as it is; blank: the standing rule). */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        String cap, String proofThreshold, String graceDays,
        java.util.Map<String, String> gradeTerms, java.util.Map<String, String> from, java.util.Map<String, String> perms) {
}
