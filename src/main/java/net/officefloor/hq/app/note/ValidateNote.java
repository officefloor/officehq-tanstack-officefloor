package net.officefloor.hq.app.note;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a note that is blank (its text trimmed); dates the rest with the sheet's today. */
public class ValidateNote {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody NoteRequest request, Sheets sheets,
            Out<Note> note) throws InvalidSheetRequestException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidSheetRequestException("A note cannot be blank");
        }
        if (text.length() > 1000) {
            throw new InvalidSheetRequestException("A note is at most 1000 characters");
        }
        String user = request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) {
            throw new InvalidSheetRequestException("A note needs the user adding it");
        }
        note.set(new Note(sheetId, text, user, sheets.today(sheetId)));
    }
}
