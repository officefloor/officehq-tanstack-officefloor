package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Writes NOTE_ADDED to the audit file once the note is stored. */
public class RecordNoteAdded {
    public void record(@PathVariable("sheetId") String sheetId, @Val NoteValues values, Audit audit) {
        audit.record("NOTE_ADDED sheet=" + sheetId.trim() + " by=" + values.by() + " text=" + values.text());
    }
}
