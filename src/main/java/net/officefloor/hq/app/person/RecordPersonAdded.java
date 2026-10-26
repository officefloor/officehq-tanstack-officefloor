package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPersonAdded {
    public void record(@Val Person person, @Val ChangedBy by, Audit audit) {
        audit.record("PERSON_ADDED sheet=" + person.getSheetId() + " by=" + by.name() + " name=" + person.getName()
                + " grade=" + person.getGrade() + (person.getTerms() == null ? "" : " terms=" + person.getTerms())
                + (person.getContact() == null ? "" : " contact=" + person.getContact()));
    }
}
