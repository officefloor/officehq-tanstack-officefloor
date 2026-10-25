package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the note, once it is added. */
public class AuditNoteAdded {
    public void audit(@Val Note note, Audit audit) {
        audit.record("NOTE_ADDED sheet=" + note.getSheetId() + " by=" + note.getByUser() + " text=" + note.getText());
    }
}
