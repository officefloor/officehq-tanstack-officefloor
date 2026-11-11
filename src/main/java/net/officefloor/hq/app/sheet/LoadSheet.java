package net.officefloor.hq.app.sheet;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.AsOf;
import net.officefloor.hq.app.Preview;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Loads the sheet. A read made as of a date (see {@link AsOf}) loads it as it stood at the end of
 * that date, the date standing in for its today; a read made at a preview date (see {@link Preview})
 * loads it as it is now, the preview date standing in for its today. A change always loads it as it
 * is now, at its real today.
 */
public class LoadSheet {
    public void load(@PathVariable("sheetId") String sheetId, SheetRepository repository,
            HttpServletRequest request, Out<Sheet> sheetOut) throws SheetNotFoundException {
        Sheet sheet = repository.find(sheetId == null ? "" : sheetId.trim());
        if (sheet == null) throw new SheetNotFoundException(sheetId);
        boolean read = "GET".equals(request.getMethod());
        LocalDate asOf = read ? AsOf.of(request) : null;
        LocalDate preview = read && asOf == null ? Preview.of(request) : null;
        if (asOf != null) sheetOut.set(repository.asOf(sheet, asOf));
        else if (preview != null) sheetOut.set(new Sheet(sheet.id(), preview, sheet.archived()));
        else sheetOut.set(sheet);
    }
}
