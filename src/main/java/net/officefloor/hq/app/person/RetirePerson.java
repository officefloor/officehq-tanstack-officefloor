package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Retires the person from the date given, or the sheet's today when none is. */
public class RetirePerson {
    public void retire(@Val RetireInput input, @Val Sheet sheet, @Val Person person,
            PersonRepository repository) {
        person.setRetiredFrom(input.from() != null ? input.from() : sheet.today());
        repository.saveRetired(person);
    }
}
