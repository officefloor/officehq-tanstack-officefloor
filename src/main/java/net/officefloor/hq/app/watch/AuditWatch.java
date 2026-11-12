package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Records a toggle: PERSON_WATCHED or PERSON_UNWATCHED sheet=<id> by=<user> name=<name>. */
public class AuditWatch {
    public void audit(@Val Watching watching, @Val Person person, Audit audit) {
        audit.record((watching.watched() ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + person.getSheetId()
                + " by=" + watching.user() + " name=" + person.getName());
    }
}
