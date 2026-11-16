package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** A person being edited may not take a name another person on the sheet already has, nor be left with a name or alias
 *  used on another sheet of the sheet's tree: a person who clashes is refused every edit until it renames them clear. */
public class CheckPersonRename {
    public void check(@Val Person person, @Val PersonValues values, PersonRepository people)
            throws DuplicatePersonNameException {
        if (people.nameTaken(person.sheetId(), values.name(), person.id())) {
            throw new DuplicatePersonNameException(values.name());
        }
        java.util.Set<String> tree = people.treeKeys(person.sheetId());
        if (tree.contains(PersonRepository.key(values.name()))) {
            throw new DuplicatePersonNameException(values.name());
        }
        for (String alias : person.aliases()) {
            if (tree.contains(PersonRepository.key(alias))) {
                throw new DuplicatePersonNameException(alias);
            }
        }
    }
}
