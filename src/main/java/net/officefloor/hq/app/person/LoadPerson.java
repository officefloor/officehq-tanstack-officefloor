package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads a person of the sheet by id. */
public class LoadPerson {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            PersonRepository repository, Out<Person> personOut) throws PersonNotFoundException {
        Long id;
        try {
            id = Long.valueOf(personId.trim());
        } catch (NumberFormatException e) {
            throw new PersonNotFoundException(personId);
        }
        personOut.set(repository.findByIdAndSheetId(id, sheetId.trim())
                .orElseThrow(() -> new PersonNotFoundException(personId)));
    }
}
