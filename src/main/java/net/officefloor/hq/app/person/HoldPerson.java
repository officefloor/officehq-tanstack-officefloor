package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Adds a hold on the person from the given date (none: the sheet's today) to the given date (none:
 *  open-ended), refusing a to-date before the from-date. */
public class HoldPerson {
    public void hold(@Val HoldInput input, @Val Person person, SheetRepository sheets) throws InvalidPersonException {
        LocalDate from = input.from() != null ? input.from()
                : sheets.findById(person.getSheetId()).orElseThrow().getToday();
        if (input.to() != null && input.to().isBefore(from)) {
            throw new InvalidPersonException("The hold to-date must not be before its from-date.");
        }
        person.addHold(new PersonHold(from, input.to()));
    }
}
