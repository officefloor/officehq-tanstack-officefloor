package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the settings are saved, so a refused change writes no audit line. A line per setting set. */
public class RecordSettingChanged {
    public void record(@Val SettingsChange change, Audit audit) {
        String prefix = "SETTING_CHANGED sheet=" + change.sheetId() + " by=" + change.by();
        if (change.setsClerkLimit()) {
            audit.record(prefix + " key=clerk-limit value="
                    + (change.clerkLimit() != null ? change.clerkLimit().toPlainString() : ""));
        }
        if (change.floor() != null) {
            audit.record(prefix + " key=floor value=" + change.floor().toPlainString());
        }
    }
}
