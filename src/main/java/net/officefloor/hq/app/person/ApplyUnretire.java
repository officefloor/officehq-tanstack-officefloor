package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Unretires the person on the sheet's today, keeping the retirement as a past one; refuses anyone not retired. */
public class ApplyUnretire {
    public void apply(@Val Sheet sheet, @Val Person person, PersonRepository repository) throws InvalidPersonException {
        if (person.getRetiredFrom() == null) {
            throw new InvalidPersonException("Only a retired person can be unretired.");
        }
        person.unretire(sheet.getToday());
        repository.saveAndFlush(person);
    }
}
