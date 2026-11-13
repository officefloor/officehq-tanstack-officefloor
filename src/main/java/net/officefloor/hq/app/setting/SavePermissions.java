package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SavePermissions {
    public void save(@Val PermissionChanges changes, @Val Sheet sheet, SettingRepository repository) {
        changes.entries().forEach((action, roles) -> repository.savePermission(sheet.id(), action, roles));
    }
}
