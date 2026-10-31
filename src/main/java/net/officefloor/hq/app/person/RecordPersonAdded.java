package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the person is saved, so a refused add writes no audit line. */
public class RecordPersonAdded {
    public void record(@Val PersonRequest request, @Val Person person, Audit audit) {
        audit.record("PERSON_ADDED sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName() + " grade=" + person.getGrade()
                + (person.getTerms() != null ? " terms=" + person.getTerms() : "")
                + (person.getContact() != null ? " contact=" + person.getContact() : ""));
    }
}
