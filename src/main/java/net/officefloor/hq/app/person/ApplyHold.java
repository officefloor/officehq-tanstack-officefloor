package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds a hold to the person: from its from-date (the sheet's today when blank) to its to-date (open-ended when
 *  blank). Refuses a to-date before the from-date. */
public class ApplyHold {
    public void apply(@Val HoldInput input, @Val Sheet sheet, @Val Person person, PersonRepository repository,
            Out<PersonHold> holdOut) throws InvalidPersonException {
        PersonHold hold = new PersonHold(input.from() == null ? sheet.getToday() : input.from(), input.to(), sheet.getToday());
        if (hold.getToDate() != null && hold.getToDate().isBefore(hold.getFromDate())) {
            throw new InvalidPersonException("The hold's to-date is before its from-date.");
        }
        person.addHold(hold);
        repository.saveAndFlush(person);
        holdOut.set(hold);
    }
}
