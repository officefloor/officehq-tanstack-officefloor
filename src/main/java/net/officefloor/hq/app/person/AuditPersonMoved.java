package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored move in the audit file. */
public class AuditPersonMoved {
    public void audit(@Val MoveInput input, @Val Person loaded, @Val MoveResult moved, Audit audit) {
        audit.record("PERSON_MOVED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " name=" + loaded.name() + " position=" + moved.position());
    }
}
