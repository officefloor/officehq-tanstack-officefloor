package net.officefloor.hq.app.sheet;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.AsOf;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Loads the sheet. A read made as of a date (see {@link AsOf}) loads it as it stood at the end of
 * that date, the date standing in for its today; a change always loads it as it is now.
 */
public class LoadSheet {
    public void load(@PathVariable("sheetId") String sheetId, SheetRepository repository,
            HttpServletRequest request, Out<Sheet> sheetOut) throws SheetNotFoundException {
        Sheet sheet = repository.find(sheetId == null ? "" : sheetId.trim());
        if (sheet == null) throw new SheetNotFoundException(sheetId);
        LocalDate asOf = "GET".equals(request.getMethod()) ? AsOf.of(request) : null;
        sheetOut.set(asOf == null ? sheet : repository.asOf(sheet, asOf));
    }
}
