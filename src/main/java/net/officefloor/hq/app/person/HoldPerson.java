package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Adds a hold to the person from the date (the sheet's today when blank) to the optional to-date.
 *  Their OPEN claims dated within it, unless bound by a statement, count in no live total; their
 *  other claims still do. A to-date before the from-date is refused. */
public class HoldPerson {
    public void hold(@Val ValidHold request, @Val Person person, People people, Sheets sheets, Audit audit,
            @Val RollupFigures before, Rollup rollup) throws InvalidPersonException {
        LocalDate from = request.from() != null ? request.from() : sheets.today(person.getSheetId());
        if (request.to() != null && request.to().isBefore(from)) {
            throw new InvalidPersonException("The hold's to-date is before its from-date");
        }
        people.addHold(person, new Person.Hold(from, request.to()));
        audit.recordOnCommit("PERSON_HELD sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName() + " from=" + from);
        rollup.recordChanges(person.getSheetId(), request.user(), before);
    }
}
