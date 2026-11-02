package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.period.PeriodCloser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Set the sheet's today, then close each open period lapsed past the sheet's grace days. */
public class SaveSheetToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today,
            ServerHttpConnection connection, SheetRepository repository, Rollup rollup, PeriodCloser closer,
            Out<Sheet> sheet)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.setToday(id, today)) {
            throw new BadRequestException("no such sheet");
        }
        closer.closeLapsed(id, by, today);
        rollup.record(id, by, before);
        sheet.set(new Sheet(id, today.toString()));
    }
}
