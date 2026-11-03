package net.officefloor.hq.app.note;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the note form: a text, trimmed, that is not blank. */
public class ValidateNote {
    public void validate(@RequestBody NoteRequest request, Out<String> note) throws BadRequestException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new BadRequestException("a note needs a text");
        }
        note.set(text);
    }
}
