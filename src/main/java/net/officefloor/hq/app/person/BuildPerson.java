package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPerson {
    public void build(@Val PersonRequest request, @Val Sheet sheet, PersonRepository repository, Out<Person> personOut) {
        personOut.set(repository.save(new Person(sheet.getId(), request.name(), request.grade())));
    }
}
