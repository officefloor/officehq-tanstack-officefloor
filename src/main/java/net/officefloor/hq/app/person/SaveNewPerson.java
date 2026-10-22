package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveNewPerson {
    public void save(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            PersonRepository repository, Out<Person> person) {
        person.set(repository.insert(sheetId.trim(), request.name(), request.grade()));
    }
}
