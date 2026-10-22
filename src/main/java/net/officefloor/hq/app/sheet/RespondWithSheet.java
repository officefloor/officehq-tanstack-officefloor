package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet: its id, its today and whether it is archived (as of the as-of date, when one is given). */
public class RespondWithSheet {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, SheetRepository repository,
            ObjectResponse<SheetResponse> response) throws SheetNotFoundException {
        String id = sheetId.trim();
        var today = repository.today(id);
        var end = AsOf.parse(asOf);
        if (today == null) {
            throw new SheetNotFoundException(id);
        }
        response.send(new SheetResponse(id, today.toString(), end != null ? repository.archivedAsOf(id, end) : repository.archived(id)));
    }
}
