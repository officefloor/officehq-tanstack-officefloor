package net.officefloor.hq.app.settings;

import java.util.Map;

/** Body of a settings change: the settings being set, each left out (null) when it is not being
 *  changed — the clerk limit (two decimals, blank for no limit), the floor (two decimals, blank for
 *  the default 1.00), the terms (whole days, blank for the default 14), the late percent (two decimals, blank for the default 0.00), the policy date (yyyy-mm-dd, blank for the sheet's today), the period numbering from (yyyy-mm-dd, blank for off), terms per grade (whole days by grade, blank for the sheet's terms) and the cap (two decimals, blank for no cap) — and who makes the change, and the from-date of a dated setting by its key (yyyy-mm-dd; left out or blank for the sheet's today), and the proof threshold (two decimals, blank for none), and the permission matrix: the roles that may take each action, by action (a comma separated list of clerk and steward, blank for the standing rule), and the grace days (whole days, blank for off; not dated). */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom, Map<String, String> gradeTerms, String user, String cap, Map<String, String> from, String proofThreshold, Map<String, String> perms, String graceDays) {
}
