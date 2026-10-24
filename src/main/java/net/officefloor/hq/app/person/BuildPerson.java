package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPerson {
    public void build(@Val PersonRequest request, @Val Sheet sheet, PersonRepository repository,
            PersonAliasRepository aliases, TreeNames tree, Out<Person> personOut)
            throws InvalidPersonException {
        personOut.set(repository.save(PersonRules.make(request, sheet, repository, aliases, tree)));
    }
}
