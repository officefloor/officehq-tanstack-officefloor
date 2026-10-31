package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Val;

/** Runs after the hold is saved: PERSON_HELD when put on hold, PERSON_RELEASED when taken off. */
public class RecordPersonHeld {
    public void record(@Val Person person, @Val Actor actor, Audit audit) {
        audit.record((person.isHeld() ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet=" + person.getSheetId()
                + " by=" + actor.user() + " name=" + person.getName());
    }
}
