package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record each saved setting in the audit file. */
public class AuditSettingChanged {
    public void audit(@Val SettingsInput input, @Val Settings settings, Audit audit) {
        audit.record("SETTING_CHANGED sheet=" + settings.sheetId() + " by=" + input.user()
                + " key=clerk-limit value=" + SettingsResponse.text(settings.clerkLimit()));
    }
}
