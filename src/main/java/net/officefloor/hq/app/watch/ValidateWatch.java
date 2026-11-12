package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateWatch {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody WatchRequest request, Out<WatchRequest> body,
            SheetStore sheets) throws InvalidSheetException {
        sheets.refuseArchived(sheetId.trim());
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidSheetException("Sign in to watch a person");
        }
        body.set(new WatchRequest(request.user().trim()));
    }
}
