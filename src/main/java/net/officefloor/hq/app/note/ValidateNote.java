package net.officefloor.hq.app.note;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a note on an archived sheet, by a viewer, or with a blank text. */
public class ValidateNote {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody NoteRequest request,
            Out<NoteInput> body, RoleStore roles) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidSheetException("A note needs a text");
        }
        body.set(new NoteInput(text, user));
    }
}
