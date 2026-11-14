package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the note added to the sheet. */
public class AuditNoteAdded {
    public void audit(@Val Note note, Audit audit) {
        audit.record("NOTE_ADDED sheet=" + note.sheetId() + " by=" + note.user() + " text=" + note.text());
    }
}
