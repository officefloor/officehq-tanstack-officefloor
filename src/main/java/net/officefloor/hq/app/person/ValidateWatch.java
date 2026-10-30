package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateWatch {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody WatchRequest request, Out<WatchRequest> body,
            Sheets sheets) throws InvalidPersonException {
        if (sheets.archived(sheetId)) {
            throw new InvalidPersonException("The sheet is archived: nothing on it can change");
        }
        if (request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Only a signed-in user can watch a person");
        }
        body.set(request);
    }
}
