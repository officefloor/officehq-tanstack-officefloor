package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records a move: PERSON_MOVED sheet=<id> by=<user> name=<name> position=<new position, counting from 1>. */
public class AuditPersonMoved {
    public void audit(@Val MoveInput input, @Val Person person, @Val Moved moved, Audit audit) {
        audit.record("PERSON_MOVED sheet=" + person.getSheetId() + " by=" + input.user() + " name=" + person.getName()
                + " position=" + moved.position());
    }
}
