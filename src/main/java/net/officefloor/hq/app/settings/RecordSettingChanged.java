package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the settings are saved, so a refused change writes no audit line. */
public class RecordSettingChanged {
    public void record(@Val SettingsChange change, Audit audit) {
        audit.record("SETTING_CHANGED sheet=" + change.sheetId() + " by=" + change.by()
                + " key=clerk-limit value=" + (change.clerkLimit() != null ? change.clerkLimit().toPlainString() : ""));
    }
}
