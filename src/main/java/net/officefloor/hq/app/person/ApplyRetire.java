package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Retires the person from the date given, or the sheet's today. */
public class ApplyRetire {
    public void apply(@Val RetireInput input, @Val Sheet sheet, @Val Person person, PersonRepository repository) {
        person.setRetiredFrom(input.date() != null ? input.date() : sheet.getToday());
        repository.saveAndFlush(person);
    }
}
