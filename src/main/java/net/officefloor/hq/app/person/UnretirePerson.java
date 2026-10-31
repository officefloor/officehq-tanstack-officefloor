package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Unretires a retired person on the sheet's today: they are no longer retired, and the retirement
 *  is kept as a past one, from its date to today. Refused: anyone not retired. Not an edit: the
 *  person's version is untouched. */
public class UnretirePerson {
    public void unretire(@Val UnretireRequest request, @Val Person person, Sheets sheets, People people,
            Out<Unretirement> unretirement) throws InvalidSheetRequestException {
        if (!person.isRetired()) {
            throw new InvalidSheetRequestException(person.getName() + " is not retired");
        }
        LocalDate today = sheets.today(person.getSheetId());
        person.unretire(today);
        people.saveRetirement(person);
        unretirement.set(new Unretirement(request.user(), today));
    }
}
