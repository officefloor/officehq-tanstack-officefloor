package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.RetiredGradeRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPerson {
    public void build(@Val PersonRequest request, @Val Sheet sheet, PersonRepository repository,
            PersonAliasRepository aliases, TreeNames tree, RetiredGradeRepository retired, Out<Person> personOut)
            throws InvalidPersonException {
        PersonRules.refuseRetired(sheet.getId(), request.grade(), retired);
        personOut.set(repository.save(PersonRules.make(request, sheet, repository, aliases, tree)));
    }
}
