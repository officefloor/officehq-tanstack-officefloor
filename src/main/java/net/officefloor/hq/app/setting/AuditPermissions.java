package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records each permission matrix entry just saved (blank: the standing rule), even when it is the one the sheet had. */
public class AuditPermissions {
    public void audit(@Val PermissionChanges changes, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        changes.entries().forEach((action, roles) -> audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user
                + " key=" + Permissions.key(action) + " value=" + roles));
    }
}
