package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSheetLoad {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody SheetRequest request,
            Out<SheetRequest> body) throws InvalidSheetException {
        String id = sheetId == null ? "" : sheetId.trim();
        if (id.isEmpty() || id.length() > 100) {
            throw new InvalidSheetException("A sheet id is required (at most 100 characters).");
        }
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidSheetException("Sign in before loading a sheet.");
        }
        body.set(request);
    }
}
