package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPerson {
    public void build(@Val SheetResponse sheet, @Val PersonRequest request, PersonRepository repository,
            Out<Person> person) {
        person.set(repository.insert(sheet.id(), request.name(), request.grade(), request.termsDays()));
    }
}
