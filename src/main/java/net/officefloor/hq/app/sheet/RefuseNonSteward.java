package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class RefuseNonSteward {
    // Only a steward (a delegation included) archives a sheet.
    public void refuse(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection, Settings settings)
            throws SheetArchiveRefusedException {
        if (!Settings.STEWARD.equals(settings.role(sheetId.trim(), Audit.user(connection).trim()))) {
            throw new SheetArchiveRefusedException("Only a steward can archive the sheet.");
        }
    }
}
