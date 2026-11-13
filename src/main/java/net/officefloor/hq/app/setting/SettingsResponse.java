package net.officefloor.hq.app.setting;

import java.util.List;

/** The settings as the screen reads them: the clerk limit (null: no limit) and each role given, by user name. */
public record SettingsResponse(String clerkLimit, List<RoleResponse> roles) {

    public record RoleResponse(String user, String role) {
    }

    public static SettingsResponse from(Settings settings) {
        return new SettingsResponse(
                settings.clerkLimit() == null ? null : settings.clerkLimit().toPlainString(),
                settings.roles().stream().map((r) -> new RoleResponse(r.user(), r.role().label())).toList());
    }
}
