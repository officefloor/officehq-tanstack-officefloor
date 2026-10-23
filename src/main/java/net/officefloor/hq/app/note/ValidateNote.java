package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a text that is not blank. */
public class ValidateNote {
    public void validate(@RequestBody NoteRequest request, Out<NoteInput> body) throws InvalidNoteException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidNoteException("Sign in before adding a note.");
        }
        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidNoteException("Give the note's text.");
        }
        body.set(new NoteInput(request.user().trim(), text));
    }
}
