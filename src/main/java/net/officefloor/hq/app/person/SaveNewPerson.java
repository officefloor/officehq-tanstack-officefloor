package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveNewPerson {
    public void save(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            PersonRepository repository, Out<Person> person) {
        Person added = repository.insert(sheetId.trim(), request.name(), request.grade(),
                request.terms() == null || request.terms().isEmpty() ? null : Integer.valueOf(request.terms()),
                request.contact() == null || request.contact().isEmpty() ? null : request.contact());
        if (request.tags() != null && !request.tags().isEmpty()) {
            repository.tags(added, request.tags());
            added = repository.find(added.sheetId(), added.id());
        }
        person.set(added);
    }
}
