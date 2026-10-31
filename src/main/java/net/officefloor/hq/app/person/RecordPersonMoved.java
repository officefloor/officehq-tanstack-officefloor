package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Val;

/** Runs after the move is saved, so a refused one writes no audit line. */
public class RecordPersonMoved {
    public void record(@Parameter Integer position, @Val Move move, @Val Person person, Audit audit) {
        audit.record("PERSON_MOVED sheet=" + person.getSheetId() + " by=" + move.user()
                + " name=" + person.getName() + " position=" + position);
    }
}
