package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class RefuseTodayPerm {
    // Only a role the permission matrix allows to change today (by default anyone but a viewer) changes it: refused before anything is read, stored or recorded.
    public void refuse(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection, Settings settings)
            throws TodayRefusedException {
        if (!settings.permits(sheetId.trim(), "today", Audit.user(connection).trim())) {
            throw new TodayRefusedException("Your role is not allowed to change the sheet's today.");
        }
    }
}
