package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Adds a hold on the person, from the date given (the sheet's today when none is) until the
 * to-date given (none: open-ended). A to-date before the from-date is refused.
 */
public class HoldPerson {
    public void hold(@Val HoldInput input, @Val Sheet sheet, @Val Person person,
            PersonRepository repository) throws InvalidPersonException {
        LocalDate from = input.from() != null ? input.from() : sheet.today();
        if (input.to() != null && input.to().isBefore(from)) {
            throw new InvalidPersonException("A hold's to-date is not before its from-date");
        }
        Hold hold = new Hold(from, input.to());
        person.getHolds().add(hold);
        repository.addHold(person, hold);
    }
}
