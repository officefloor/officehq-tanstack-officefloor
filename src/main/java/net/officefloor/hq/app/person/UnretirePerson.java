package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Unretires the person: their retirement is kept as a past one, to the sheet's today. */
public class UnretirePerson {
    public void unretire(@Val Sheet sheet, @Val Person person, PersonRepository repository) {
        PastRetirement past = new PastRetirement(person.getRetiredFrom(), sheet.today());
        person.getPastRetirements().add(past);
        person.setRetiredFrom(null);
        repository.saveUnretired(person, past);
    }
}
