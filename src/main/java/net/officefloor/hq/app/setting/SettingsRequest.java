package net.officefloor.hq.app.setting;

/**
 * Settings as the screen sends them, each only when it is being changed (null: left as it is): the
 * clerk limit, with up to two decimals (blank: no limit), the floor, with up to two decimals, and
 * the terms, a whole number of days, and the late percent, with up to two decimals, and the policy
 * date as yyyy-MM-dd (blank: the sheet's today), and the period numbering from as yyyy-MM-dd
 * (blank: off), and the terms of each grade being changed, a whole number of days (blank: the
 * sheet's terms), and the cap, with up to two decimals (blank: none).
 */
public record SettingsRequest(String clerkLimit, String floor, String terms, String latePct, String policyDate, String periodNumberingFrom,
        java.util.Map<String, String> gradeTerms, String cap) {
}
