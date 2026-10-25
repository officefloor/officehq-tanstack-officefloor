package net.officefloor.hq.app.note;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Adds the note to the sheet, by the signed-in name, on the sheet's today. */
public class ApplyNote {
    public void apply(@Val NoteText text, @Val Sheet sheet,
            @RequestHeader(name = "X-User", required = false) String user, NoteRepository repository,
            Out<Note> noteOut) {
        noteOut.set(repository.saveAndFlush(new Note(sheet.getId(), text.value(), AuditUser.decode(user), sheet.getToday())));
    }
}
