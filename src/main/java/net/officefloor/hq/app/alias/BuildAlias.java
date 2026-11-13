package net.officefloor.hq.app.alias;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.NameTree;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The person's new alias; refused (400) when it equals, ignoring case, any name or alias on the sheet or on
 * any other sheet of its tree. */
public class BuildAlias {
    public void build(@Val Person person, @Val AliasRequest request, PersonRepository people, NameTree tree,
            Out<Alias> alias)
            throws InvalidPersonException {
        if (people.nameTaken(person.sheetId(), request.alias(), null) || tree.clashes(person.sheetId(), request.alias())) {
            throw new InvalidPersonException("The name " + request.alias() + " is already in use.");
        }
        alias.set(new Alias(person.sheetId(), person.id(), request.alias()));
    }
}
