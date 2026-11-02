package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadSheetToday {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            SheetRepository repository, Out<Sheet> sheet) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        try {
            sheet.set(new Sheet(id, repository.today(id).toString(), repository.archived(id)));
        } catch (EmptyResultDataAccessException e) {
            throw new BadRequestException("no such sheet");
        }
    }
}
