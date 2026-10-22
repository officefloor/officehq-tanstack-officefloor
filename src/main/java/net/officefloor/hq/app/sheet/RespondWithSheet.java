package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSheet {
    public void respond(@PathVariable("sheetId") String sheetId, SheetRepository repository,
            ObjectResponse<SheetResponse> response) throws SheetNotFoundException {
        String id = sheetId.trim();
        var today = repository.today(id);
        if (today == null) {
            throw new SheetNotFoundException(id);
        }
        response.send(new SheetResponse(id, today.toString()));
    }
}
