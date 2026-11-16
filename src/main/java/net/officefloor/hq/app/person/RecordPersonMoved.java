package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_MOVED, with the person's new position (counting from 1), once the move is stored. */
public class RecordPersonMoved {
    public void record(@Val Person person, @Val PersonMove move, Audit audit) {
        audit.record("PERSON_MOVED sheet=" + person.sheetId() + " by=" + move.by() + " name=" + person.name()
                + " position=" + move.position());
    }
}
