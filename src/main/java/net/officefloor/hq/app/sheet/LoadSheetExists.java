package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadSheetExists {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            SheetRepository repository, Out<SheetExists> exists) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        exists.set(new SheetExists(!id.isEmpty() && repository.exists(id)));
    }
}
