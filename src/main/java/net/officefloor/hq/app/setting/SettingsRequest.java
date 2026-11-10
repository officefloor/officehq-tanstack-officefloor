package net.officefloor.hq.app.setting;

/**
 * Settings as the screen sends them, each only when it is being changed (null: left as it is): the
 * clerk limit, with up to two decimals (blank: no limit), the floor, with up to two decimals, and
 * the terms, a whole number of days, and the late percent, with up to two decimals, and the policy
 * date as yyyy-MM-dd (blank: the sheet's today), and the period numbering from as yyyy-MM-dd
 * (blank: off), and the terms of each grade being changed, a whole number of days (blank: the
 * sheet's terms), and the cap, with up to two decimals (blank: none), and the from-date of each
 * figure setting being changed, by its key (clerk-limit, floor, terms, late-pct, terms-A, terms-B,
 * terms-C, cap, proof-threshold, note-threshold), as yyyy-MM-dd (blank or missing: the sheet's today), and the proof
 * threshold, with up to two decimals (blank: none), and the entry of each action of the permission
 * matrix being changed, by action (open, settle, void, reassign, close, reopen), a comma separated
 * list of roles, clerk or steward (blank: the standing rule), and the grace days, a whole number of
 * days (blank: off), and the note threshold, with up to two decimals (blank: none), and the entry date as yyyy-MM-dd
 * (blank: off), and the grace days of each grade being changed, a whole number of days (blank:
 * none), dated by its key (grace-A, grace-B, grace-C).
 */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        java.util.Map<String, String> gradeTerms, String cap, java.util.Map<String, String> from,
        String proofThreshold, java.util.Map<String, String> perms, String graceDays, String noteThreshold,
        String entryDate, java.util.Map<String, String> gradeGrace) {
}
