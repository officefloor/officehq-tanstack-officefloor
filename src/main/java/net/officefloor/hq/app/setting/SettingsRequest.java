package net.officefloor.hq.app.setting;

import java.util.Map;

/** Body of a settings save: who is making it and the settings it changes — the clerk limit (blank: no limit) and
 *  the floor and the terms (days) and the late percent and the policy date (yyyy-mm-dd; blank: the sheet's today) and the period numbering from (yyyy-mm-dd; blank: off) and grades' own terms (days by grade;
 *  blank: none, the sheet's terms) and the cap (blank: none); one left out (null) is left as it is. */
public record SettingsRequest(String user, String clerkLimit, String floor, String terms, String latePct,
        String policyDate, String periodNumberingFrom, Map<String, String> gradeTerms, String cap) {
}
