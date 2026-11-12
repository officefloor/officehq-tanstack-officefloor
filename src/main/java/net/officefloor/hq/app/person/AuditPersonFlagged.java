package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records a flag: PERSON_FLAGGED sheet=<id> by=<user> name=<name> reason=<reason> until=<date>. */
public class AuditPersonFlagged {
    public void audit(@Val FlagInput input, @Val Person person, Audit audit) {
        audit.record("PERSON_FLAGGED sheet=" + person.getSheetId() + " by=" + input.user() + " name="
                + person.getName() + " reason=" + input.flag().reason() + " until=" + input.flag().until());
    }
}
