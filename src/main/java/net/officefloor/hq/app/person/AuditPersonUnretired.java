package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored unretire in the audit file, with the day it was made on (the end of the retirement). */
public class AuditPersonUnretired {
    public void audit(@Val UnretireInput input, @Val Person loaded, @Val PersonResponse saved, Audit audit) {
        var ended = saved.retirements().get(saved.retirements().size() - 1);
        audit.record("PERSON_UNRETIRED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " name=" + saved.name() + " on=" + ended.to());
    }
}
