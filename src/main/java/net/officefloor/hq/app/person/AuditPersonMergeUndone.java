package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person a merge's undo restored and the person they had been merged into. */
public class AuditPersonMergeUndone {
    public void audit(@Val MergeMade made, @Val Person person, @RequestParam("user") String user, Audit audit) {
        audit.record("PERSON_MERGE_UNDONE sheet=" + person.sheetId() + " by=" + user + " name=" + made.name()
                + " from=" + person.name());
    }
}
