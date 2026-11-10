package net.officefloor.hq.app.setting;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Records each saved setting (blank for no clerk limit). Runs after every rule has passed, so a
 * refused change writes nothing.
 */
public class AuditSettingChanged {
    public void audit(@Val Sheet sheet, @Val Settings settings, HttpServletRequest request, Audit audit) {
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " key=clerk-limit value=" + SettingsResponse.from(settings).clerkLimit());
    }
}
