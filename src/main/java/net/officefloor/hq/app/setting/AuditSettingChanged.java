package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record each saved setting in the audit file, a line per setting the save sets. */
public class AuditSettingChanged {
    public void audit(@Val SettingsInput input, @Val Settings settings, Audit audit) {
        if (input.setsClerkLimit()) {
            audit.record("SETTING_CHANGED sheet=" + settings.sheetId() + " by=" + input.user()
                    + " key=clerk-limit value=" + SettingsResponse.text(settings.clerkLimit()));
        }
        if (input.floor() != null) {
            audit.record("SETTING_CHANGED sheet=" + settings.sheetId() + " by=" + input.user()
                    + " key=floor value=" + SettingsResponse.text(settings.floor()));
        }
        if (input.terms() != null) {
            audit.record("SETTING_CHANGED sheet=" + settings.sheetId() + " by=" + input.user()
                    + " key=terms value=" + settings.terms());
        }
    }
}
