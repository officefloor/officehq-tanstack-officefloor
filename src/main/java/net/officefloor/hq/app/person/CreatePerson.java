package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class CreatePerson {
    public void create(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            PersonRepository repository, Out<Person> person) {
        person.set(repository.create(sheetId, request.name(), request.grade()));
    }
}
