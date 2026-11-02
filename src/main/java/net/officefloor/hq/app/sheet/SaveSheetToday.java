package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveSheetToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today,
            ServerHttpConnection connection, SheetRepository repository, Audit audit, Out<Sheet> sheet)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.setToday(id, today)) {
            throw new BadRequestException("no such sheet");
        }
        audit.record("sheet-today sheet=" + id + " by=" + by + " date=" + today);
        sheet.set(new Sheet(id, today.toString()));
    }
}
