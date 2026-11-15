package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a change that went through is recorded. */
public class RecordPersonAdded {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Person person, Audit audit) {
        audit.record("PERSON_ADDED sheet=" + person.getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " grade=" + person.getGrade()
                + (person.getTerms() == null ? "" : " terms=" + person.getTerms())
                + (person.getContact() == null ? "" : " contact=" + person.getContact()));
    }
}
