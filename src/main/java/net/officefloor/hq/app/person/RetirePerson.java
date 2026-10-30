package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Retires the person from the date (the sheet's today when blank). Once the sheet's today reaches the date,
 *  their OPEN and PENDING claims, unless bound by a statement, count in no live total; their SETTLED claims still do. */
public class RetirePerson {
    public void retire(@Val ValidRetire request, @Val Person person, People people, Sheets sheets, Audit audit,
            @Val RollupFigures before, Rollup rollup) {
        LocalDate from = request.date() != null ? request.date() : sheets.today(person.getSheetId());
        person.setRetiredFrom(from);
        people.saveRetired(person);
        audit.recordOnCommit("PERSON_RETIRED sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName() + " from=" + from);
        rollup.recordChanges(person.getSheetId(), request.user(), before);
    }
}
