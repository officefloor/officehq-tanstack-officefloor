package net.officefloor.hq.app.settings;

import net.officefloor.plugin.variable.Val;

public class SaveSettings {
    public void save(@Val SettingsChange change, Settings settings) {
        settings.setClerkLimit(change.sheetId(), change.clerkLimit());
    }
}
