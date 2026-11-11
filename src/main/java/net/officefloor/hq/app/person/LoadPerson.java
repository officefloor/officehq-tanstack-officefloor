package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPerson {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            PersonStore store, Out<Person> personOut) throws InvalidSheetException {
        Person person = null;
        try {
            person = store.find(sheetId.trim(), Long.parseLong(personId.trim()));
        } catch (NumberFormatException e) {
            // not an id: no such person
        }
        if (person == null) {
            throw new InvalidSheetException("No such person: " + personId);
        }
        personOut.set(person);
    }
}
