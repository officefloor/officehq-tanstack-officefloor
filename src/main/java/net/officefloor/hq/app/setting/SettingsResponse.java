package net.officefloor.hq.app.setting;

import java.util.List;

/** The settings as the screen reads them: the clerk limit (null: no limit), the floor, the terms in days, the late percent, the policy date ("": blank, the sheet's today) and each role given, by user name. */
public record SettingsResponse(String clerkLimit, String floor, int terms, String latePct, String policyDate, List<RoleResponse> roles) {

    public record RoleResponse(String user, String role) {
    }

    public static SettingsResponse from(Settings settings) {
        return new SettingsResponse(
                settings.clerkLimit() == null ? null : settings.clerkLimit().toPlainString(),
                settings.floor().toPlainString(),
                settings.terms(),
                settings.latePct().toPlainString(),
                settings.policyDate() == null ? "" : settings.policyDate().toString(),
                settings.roles().stream().map((r) -> new RoleResponse(r.user(), r.role().label())).toList());
    }
}
