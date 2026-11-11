package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A note needs text; it is kept trimmed. */
public class ValidateNote {
    public void validate(@RequestBody NoteRequest request, Out<NoteInput> body) throws InvalidNoteException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) throw new InvalidNoteException("A note needs text");
        body.set(new NoteInput(text));
    }
}
