package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class BuildPerson {
    public void build(@PathVariable("sheet") String sheet, @Val PersonRequest request, People people,
            Out<Person> person) throws DuplicatePersonNameException {
        person.set(people.add(sheet, request.name(), request.grade()));
    }
}
