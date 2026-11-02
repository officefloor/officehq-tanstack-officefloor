package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SavePerson {
    public void save(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @Val PersonRequest request, ServerHttpConnection connection, PersonRepository repository,
            Audit audit, Out<Person> person) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person saved = new Person(pid, request.name(), request.grade());
        if (!repository.update(id, saved)) {
            throw new BadRequestException("no such person");
        }
        audit.record("person-edit sheet=" + id + " by=" + by + " person=" + pid
                + " grade=" + saved.grade() + " name=" + saved.name());
        person.set(saved);
    }
}
