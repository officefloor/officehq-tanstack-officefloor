package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses a person whose name another person on the sheet already has, ignoring case, or whose contact another has;
 * and one whose name any other sheet of the sheet's tree uses as a name or alias. An edit of someone already in use
 * across the tree is refused until it renames them to a name that is not, while none of their aliases is.
 */
public class CheckPersonNameUnique {
    public void check(@Val Person person, PersonRepository repository, NameTree tree) throws InvalidPersonException {
        if (repository.nameTaken(person.sheetId(), person.name(), person.id())) {
            throw new InvalidPersonException("The name " + person.name() + " is already in use.");
        }
        if (person.contact() != null && repository.contactTaken(person.sheetId(), person.contact(), person.id())) {
            throw new InvalidPersonException("The contact " + person.contact() + " is already in use.");
        }
        List<String> aliases = person.id() == null ? List.of() : repository.aliases(person.sheetId(), person.id());
        if (NameTree.clashing(tree.taken(person.sheetId()), person.name(), aliases)) {
            throw new InvalidPersonException("The name " + person.name() + " or an alias is already in use on a related sheet.");
        }
    }
}
