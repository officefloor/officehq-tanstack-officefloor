package net.officefloor.hq.app.sheet;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.ViewerException;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses a change of the sheet's today by a role the permission matrix does not allow (by the
 * standing rule, anyone but a viewer), before anything is written.
 */
public class CheckTodayPermission {
    public void check(@Val Sheet sheet, RoleRepository roles, SettingsRepository settings, HttpServletRequest request)
            throws ViewerException {
        String role = roles.roleOf(sheet.id(), Audit.user(request));
        if (!settings.find(sheet.id()).allows("today", role)) {
            throw new ViewerException("A " + role + " may not change the sheet's today");
        }
    }
}
