package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Unretires a retired person on the sheet's today: they are no longer retired, and the retirement is kept as a past
 *  one from its from-date to that day. Anyone not retired is refused. */
public class UnretirePerson {
    public void unretire(@Val ValidUnretire request, @Val Person person, People people, Sheets sheets, Audit audit,
            @Val RollupFigures before, Rollup rollup) throws InvalidPersonException {
        if (!person.isRetired()) {
            throw new InvalidPersonException("The person is not retired");
        }
        LocalDate on = sheets.today(person.getSheetId());
        people.saveUnretired(person, on);
        audit.recordOnCommit("PERSON_UNRETIRED sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName() + " on=" + on);
        rollup.recordChanges(person.getSheetId(), request.user(), before);
    }
}
