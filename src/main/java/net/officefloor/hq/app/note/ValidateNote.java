package net.officefloor.hq.app.note;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the text, trimmed, is not blank. */
public class ValidateNote {
    public void validate(@RequestBody NoteRequest request, Out<NoteValues> values) throws InvalidNoteException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidNoteException("A note must not be blank");
        }
        values.set(new NoteValues(text, request.by() == null ? "" : request.by().trim()));
    }
}
