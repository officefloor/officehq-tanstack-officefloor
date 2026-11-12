package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Records a stored person edit: PERSON_EDITED sheet=<id> by=<user> name=<name> grade=<grade>, the grade
 *  in force on the sheet's today after the save, then terms=<days> when the edit changed their terms (empty when
 *  it cleared them). */
public class AuditPersonEdited {
    public void audit(@Val PersonRequest request, @Val Person person, @Val Rollup before, Audit audit,
            RollupService rollups, SheetStore sheets) {
        audit.record("PERSON_EDITED sheet=" + person.getSheetId() + " by=" + request.user() + " name="
                + person.getName() + " grade=" + person.gradeOn(sheets.today(person.getSheetId()))
                + (person.isTermsChanged() ? " terms=" + (person.getTerms() == null ? "" : person.getTerms()) : ""));
        rollups.record(person.getSheetId(), request.user(), before);
    }
}
