package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse any change by a viewer of the sheet, and any change at all to an archived sheet (400, nothing written). The first step of every endpoint
 *  that changes a sheet, except the bulk ones, where a viewer's selection is simply all refused. */
public class RefuseViewer {
    public void refuse(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            SettingsRepository settings) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        if (settings.role(id, by).equals(SettingsRepository.VIEWER)) {
            throw new BadRequestException("a viewer can change nothing");
        }
    }
}
