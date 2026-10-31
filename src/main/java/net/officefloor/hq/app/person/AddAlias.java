package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Gives the person an alias. The person is read afresh and locked, so two adds at once happen one
 *  after the other. Not an edit: the person's version is untouched. Refuses an alias equal, as names
 *  are compared, to any name or alias on the sheet. Publishes the person as they are now. */
public class AddAlias {
    public void add(@PathVariable("sheetId") String sheetId, @PathVariable("personId") long personId,
            @Val AliasRequest request, People people, Out<Person> personOut, Out<Aliasing> aliasingOut)
            throws PersonNotFoundException, InvalidSheetRequestException {
        Person person = people.lock(sheetId, personId);
        if (person == null) throw new PersonNotFoundException(personId);
        String alias = request.alias().trim();
        if (people.nameOrAliasTaken(sheetId, alias)) {
            throw new InvalidSheetRequestException("The alias is already in use on this sheet");
        }
        people.addAlias(person, alias);
        personOut.set(person);
        aliasingOut.set(new Aliasing(person, alias, request.user()));
    }
}
