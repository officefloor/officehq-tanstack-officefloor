package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Val;

/** Runs after the hold is saved: PERSON_HELD with the hold's from-date. */
public class RecordPersonHeld {
    public void record(@Val Person person, @Val Hold hold, @Val Actor actor, Audit audit) {
        audit.record("PERSON_HELD sheet=" + person.getSheetId() + " by=" + actor.user() + " name=" + person.getName()
                + " from=" + hold.from());
    }
}
