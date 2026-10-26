package net.officefloor.hq.app.note;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A note's text (trimmed, required). */
public class ValidateNote {

    public void validate(@RequestBody NoteRequest request, Out<NoteInput> note, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new BadRequestException("Enter a note");
        }
        if (text.length() > 2000) {
            throw new BadRequestException("A note is at most 2000 characters");
        }
        note.set(new NoteInput(text));
        changedBy.set(by);
    }
}
