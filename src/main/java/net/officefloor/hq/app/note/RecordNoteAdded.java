package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordNoteAdded {
    public void record(@Val Note note, @Val ChangedBy by, Audit audit) {
        audit.record("NOTE_ADDED sheet=" + note.sheetId() + " by=" + by.name() + " text=" + note.text());
    }
}
