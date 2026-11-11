package net.officefloor.hq.app.note;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the note added. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditNoteAdded {
    public void audit(@Val Note note, HttpServletRequest request, Audit audit) {
        audit.record("NOTE_ADDED sheet=" + note.sheetId() + " by=" + Audit.user(request) + " text=" + note.text());
    }
}
