package net.officefloor.hq.app.note;

import jakarta.validation.Valid;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) a note that is blank (once trimmed); else the note, dated the sheet's today. */
public class ValidateNoteRequest {
    public void validate(@Val Sheet sheet, @RequestBody @Valid NoteRequest request, @RequestParam("user") String user,
            Out<Note> note) throws InvalidNoteException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidNoteException("A note cannot be blank.");
        }
        note.set(new Note(sheet.id(), sheet.today(), user, text));
    }
}
