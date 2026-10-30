package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

/** Puts the person on hold, or releases them when already held. While held their OPEN claims count
 *  in no live total; their SETTLED claims still do. */
public class HoldPerson {
    public void hold(@Val HoldRequest request, @Val Person person, People people, Audit audit,
            @Val RollupFigures before, Rollup rollup) {
        person.setHeld(!person.isHeld());
        people.saveHeld(person);
        audit.recordOnCommit((person.isHeld() ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet=" + person.getSheetId()
                + " by=" + request.user() + " name=" + person.getName());
        rollup.recordChanges(person.getSheetId(), request.user(), before);
    }
}
