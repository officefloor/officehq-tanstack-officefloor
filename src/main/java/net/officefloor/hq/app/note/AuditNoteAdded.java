package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a note added: NOTE_ADDED sheet=<id> by=<user> text=<text>. */
public class AuditNoteAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val NoteInput note, Audit audit) {
        audit.record("NOTE_ADDED sheet=" + sheetId.trim() + " by=" + note.user() + " text=" + note.text());
    }
}
