package net.officefloor.hq.app.settings;

import net.officefloor.plugin.variable.Val;

public class SaveSettings {
    public void save(@Val SettingsChange change, Settings settings) {
        if (change.setsClerkLimit()) settings.setClerkLimit(change.sheetId(), change.clerkLimit());
        if (change.floor() != null) settings.setFloor(change.sheetId(), change.floor());
    }
}
