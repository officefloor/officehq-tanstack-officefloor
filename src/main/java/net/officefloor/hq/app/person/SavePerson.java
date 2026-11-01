package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SavePerson {
    public void save(@PathVariable("sheet") String sheet, @Val Person person, @Val PersonRequest request,
            People people) throws DuplicatePersonNameException {
        people.update(sheet, new Person(person.id(), request.name(), request.grade()));
    }
}
