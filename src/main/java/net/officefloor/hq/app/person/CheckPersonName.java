package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses a person whose name another person on the sheet already has, or whose name or any alias is
 * a name or alias on another sheet of the sheet's tree (so a person who clashes is refused every edit
 * until it leaves them clashing no more).
 */
public class CheckPersonName {
    public void check(@Val Person person, PersonRepository repository, SheetRepository sheets)
            throws DuplicatePersonNameException {
        if (repository.nameTaken(person.getSheetId(), person.getName(), person.getId())) {
            throw new DuplicatePersonNameException(person.getName());
        }
        Set<String> tree = sheets.tree(person.getSheetId());
        if (repository.nameTakenIn(tree, person.getName(), person.getId())) {
            throw new DuplicatePersonNameException(person.getName());
        }
        for (String alias : person.getAliases()) {
            if (repository.nameTakenIn(tree, alias, person.getId())) throw new DuplicatePersonNameException(alias);
        }
    }
}
