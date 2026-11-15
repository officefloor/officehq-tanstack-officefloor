package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a note that went through is recorded. */
public class RecordNoteAdded {
    public void record(@Val Note note, Audit audit) {
        audit.record("NOTE_ADDED sheet=" + note.sheetId() + " by=" + note.by() + " text=" + note.text());
    }
}
