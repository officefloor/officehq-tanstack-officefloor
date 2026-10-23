package net.officefloor.hq.app.note;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored note in the audit file. */
public class AuditNoteAdded {
    public void audit(@Val Note n, Audit audit) {
        audit.record("NOTE_ADDED sheet=" + n.sheetId() + " by=" + n.by() + " text=" + n.text());
    }
}
