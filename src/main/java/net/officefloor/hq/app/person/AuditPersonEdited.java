package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;

public class AuditPersonEdited {
    public void audit(@Val PersonRequest request, @Val Person person, Audit audit, @Val RollupFigures before, Rollup rollup) {
        audit.recordOnCommit("PERSON_EDITED sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName() + " grade=" + person.getGrade()
                + (person.isTermsChanged() ? " terms=" + (person.getTerms() == null ? "" : person.getTerms()) : ""));
        rollup.recordChanges(person.getSheetId(), request.user(), before);
    }
}
