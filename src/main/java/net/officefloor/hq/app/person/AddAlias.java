package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Gives the person one more alias: refused when any name or alias on the sheet already is it (ignoring case). */
public class AddAlias {
    public void add(@Val AliasInput input, @Val Sheet sheet, @Val Person person,
            PersonRepository repository) throws DuplicatePersonNameException {
        if (repository.nameTaken(sheet.id(), input.alias(), null)) {
            throw new DuplicatePersonNameException(input.alias());
        }
        repository.addAlias(person, input.alias());
    }
}
