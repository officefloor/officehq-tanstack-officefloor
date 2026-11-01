package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the note is saved, so a refused one writes no audit line. */
public class RecordNoteAdded {
    public void record(@Val Note note, Audit audit) {
        audit.record("NOTE_ADDED sheet=" + note.sheetId() + " by=" + note.by() + " text=" + note.text());
    }
}
