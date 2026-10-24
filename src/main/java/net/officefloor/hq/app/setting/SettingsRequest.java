package net.officefloor.hq.app.setting;

/** The settings form as submitted: the clerk limit (blank: no limit), the floor (blank: 1.00) and the terms in
 *  days (blank: 14) and the late percent
 *  (blank: 0.00) and the policy date (YYYY-MM-DD; blank: the sheet's today) and the period numbering from (YYYY-MM-DD; blank: off) and the terms of grades A, B and C in days (blank: the sheet's terms) and the cap (blank: none); a key left out (null) is not changed.
 *  Each setting that carries a figure has its from-date (YYYY-MM-DD; blank or left out: the sheet's today): it is
 *  recorded from that date on. */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        String termsA, String termsB, String termsC, String cap,
        String clerkLimitFrom, String floorFrom, String termsFrom, String latePctFrom,
        String termsAFrom, String termsBFrom, String termsCFrom, String capFrom) {
}
