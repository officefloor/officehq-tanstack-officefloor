package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPerson {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            PersonRepository repository, Out<Person> personOut) throws PersonNotFoundException {
        personOut.set(repository.findByIdAndSheetId(personId, sheetId.trim())
                .orElseThrow(() -> new PersonNotFoundException("No such person on this sheet.")));
    }
}
