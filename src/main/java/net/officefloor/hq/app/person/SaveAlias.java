package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Give the loaded person the alias, refused when it equals, as names are compared, any name or alias on the sheet
 *  or on another sheet of its tree.
 *  Leaves the version alone: an alias is not an edit of the person. */
public class SaveAlias {
    public void save(@Val AliasInput input, @Val Person loaded, PersonRepository repository, Out<PersonResponse> saved)
            throws DuplicatePersonNameException {
        if (repository.nameInUse(loaded.sheetId(), input.alias(), null)) {
            throw new DuplicatePersonNameException(input.alias());
        }
        DuplicatePersonNameException.refuseInTree(loaded.sheetId(), input.alias(), repository);
        repository.alias(loaded, input.alias());
        saved.set(PersonResponse.from(repository.find(loaded.sheetId(), loaded.id())));
    }
}
