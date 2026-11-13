package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a clone that names no sheet to clone to, or an id too long; publishes it with the id trimmed. */
public class ValidateSheetCloneRequest {
    public void validate(@RequestBody SheetCloneRequest request, Out<SheetCloneRequest> clone)
            throws SheetCloneRefusedException {
        String to = request.to() == null ? "" : request.to().trim();
        if (to.isEmpty()) {
            throw new SheetCloneRefusedException("Enter the sheet to clone to.");
        }
        if (to.length() > 100) {
            throw new SheetCloneRefusedException("A sheet id is at most 100 characters.");
        }
        clone.set(new SheetCloneRequest(to));
    }
}
