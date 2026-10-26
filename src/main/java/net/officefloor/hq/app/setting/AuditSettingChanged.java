package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditSettingChanged {
    public void audit(@Val SheetSettings settings, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("SETTING_CHANGED", settings.sheetId(), Audit.user(connection),
                "key=" + Settings.CLERK_LIMIT, "value=" + settings.clerkLimit()));
    }
}
