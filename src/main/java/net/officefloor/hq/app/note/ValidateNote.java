package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the note's text from the body, trimmed; refuses a blank one. */
public class ValidateNote {
    public void validate(@RequestBody NoteRequest request, Out<NoteText> text) throws InvalidNoteException {
        String value = request == null || request.text() == null ? "" : request.text().trim();
        if (value.isEmpty()) {
            throw new InvalidNoteException("A note must have a text.");
        }
        text.set(new NoteText(value));
    }
}
