package net.officefloor.hq.app.setting;

/** The settings form as submitted: the clerk limit (blank: no limit), the floor (blank: 1.00) and the terms in
 *  days (blank: 14) and the late percent
 *  (blank: 0.00) and the policy date (YYYY-MM-DD; blank: the sheet's today) and the period numbering from (YYYY-MM-DD; blank: off); a key left out (null) is not changed. */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom) {
}
