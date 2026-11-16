package net.officefloor.hq.app.patron;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import net.officefloor.hq.app.sheet.AsOf;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Every patron named on the sheet; read as of a date (see AsOf), as they stood then. */
public class RespondWithPatrons {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, PatronRepository patrons,
            ObjectResponse<List<Patron>> response) {
        java.time.LocalDate date = AsOf.parse(asOf);
        response.send(date == null ? patrons.list(sheetId.trim()) : patrons.listAsOf(sheetId.trim(), date));
    }
}
