package net.officefloor.hq.app.settings;

import net.officefloor.plugin.variable.Val;

public class SaveRole {
    public void save(@Val RoleChange change, Settings settings) {
        settings.setRole(change.sheetId(), change.name(), change.role());
    }
}
