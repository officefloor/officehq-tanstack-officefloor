package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Flags the person with the reason until the date, replacing any flag they had. */
public class FlagPerson {
    public void flag(@Val ValidFlag request, @Val Person person, People people, Audit audit) {
        people.flag(person, request.reason(), request.until());
        audit.recordOnCommit("PERSON_FLAGGED sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName() + " reason=" + request.reason() + " until=" + request.until());
    }
}
