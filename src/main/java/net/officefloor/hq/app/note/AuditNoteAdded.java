package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

public class AuditNoteAdded {
    public void audit(@Val Note note, Audit audit) {
        audit.record(Audit.line("NOTE_ADDED", note.sheetId(), note.by(), "text=" + note.text()));
    }
}
