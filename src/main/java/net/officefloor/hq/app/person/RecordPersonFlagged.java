package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Val;

/** Runs after the flag is saved: PERSON_FLAGGED with its reason and until-date. */
public class RecordPersonFlagged {
    public void record(@Val Person person, @Val Actor actor, Audit audit) {
        audit.record("PERSON_FLAGGED sheet=" + person.getSheetId() + " by=" + actor.user() + " name=" + person.getName()
                + " reason=" + person.getFlagReason() + " until=" + person.getFlagUntil());
    }
}
