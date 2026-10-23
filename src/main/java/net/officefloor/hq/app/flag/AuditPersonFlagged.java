package net.officefloor.hq.app.flag;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

/** Record the stored flag in the audit file, with its reason and until date. */
public class AuditPersonFlagged {
    public void audit(@Val FlagInput input, @Val Person loaded, Audit audit) {
        audit.record("PERSON_FLAGGED sheet=" + loaded.sheetId() + " by=" + input.user() + " name=" + loaded.name()
                + " reason=" + input.reason() + " until=" + input.until());
    }
}
