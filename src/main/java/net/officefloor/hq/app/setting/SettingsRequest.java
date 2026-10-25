package net.officefloor.hq.app.setting;

/** The settings form as submitted: the clerk limit (blank: no limit), the floor (blank: 1.00) and the terms in
 *  days (blank: 14) and the late percent
 *  (blank: 0.00) and the policy date (YYYY-MM-DD; blank: the sheet's today) and the period numbering from (YYYY-MM-DD; blank: off) and the terms of grades A, B and C in days (blank: the sheet's terms) and the cap (blank: none) and the proof threshold (blank: none); and the permission matrix entries for open, settle, void, reassign, close and reopen
 *  (comma separated roles; blank: the standing rule); and the grace days (a whole number of days; blank: off; not dated); and the note threshold (blank: none); and the entry date (YYYY-MM-DD; blank: off; not dated); and the grace days of grades A, B and C (blank: none); and the proof thresholds of grades A, B and C (blank: none — the sheet's applies); and the pot (two decimals; blank: none; not dated); and the parent sheet (a sheet's id; blank: none; not dated); and the fiscal year start month (1 to 12; blank: 1; not dated); and the alert threshold (two decimals; blank: off; not dated); and the rounding mode (half-up or half-even; blank: none — a claim rounds by the policy date); and the review threshold (blank: none); and the escalation gap (a whole number of days; blank: off; not dated); and the review SLA (a whole number of days; blank: none; not dated); and the proof kinds (a comma separated list of receipt and photo; blank: none; not dated); a key left out (null) is not changed.
 *  Each setting that carries a figure has its from-date (YYYY-MM-DD; blank or left out: the sheet's today): it is
 *  recorded from that date on. */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        String termsA, String termsB, String termsC, String cap, String proofThreshold,
        String clerkLimitFrom, String floorFrom, String termsFrom, String latePctFrom,
        String termsAFrom, String termsBFrom, String termsCFrom, String capFrom, String proofThresholdFrom,
        String permOpen, String permSettle, String permVoid, String permReassign, String permClose, String permReopen,
        String graceDays, String noteThreshold, String noteThresholdFrom, String entryDate,
        String graceA, String graceB, String graceC, String graceAFrom, String graceBFrom, String graceCFrom,
        String proofThresholdA, String proofThresholdB, String proofThresholdC,
        String proofThresholdAFrom, String proofThresholdBFrom, String proofThresholdCFrom, String pot, String parent,
        String fiscalStartMonth, String alertThreshold, String rounding, String roundingFrom,
        String reviewThreshold, String reviewThresholdFrom, String escalateAfter, String reviewSla, String proofKinds) {
}
