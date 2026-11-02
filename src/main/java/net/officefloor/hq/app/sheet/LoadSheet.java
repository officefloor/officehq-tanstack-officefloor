package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadSheet {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            SheetRepository repository, Out<Sheet> sheet) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (id.isEmpty()) {
            throw new BadRequestException("sheet id is required");
        }
        sheet.set(repository.loadOrCreate(id, by));
    }
}
