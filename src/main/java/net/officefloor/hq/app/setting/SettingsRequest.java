package net.officefloor.hq.app.setting;

/** Body of a settings save: who is making it and the settings it changes — the clerk limit (blank: no limit) and
 *  the floor and the terms (days) and the late percent and the policy date (yyyy-mm-dd; blank: the sheet's today); one left out (null) is left as it is. */
public record SettingsRequest(String user, String clerkLimit, String floor, String terms, String latePct,
        String policyDate) {
}
