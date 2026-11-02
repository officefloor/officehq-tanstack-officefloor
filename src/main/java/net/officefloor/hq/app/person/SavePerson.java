package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.dao.DuplicateKeyException;
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
        if (repository.nameTaken(id, request.name(), pid)) {
            throw new BadRequestException("name is already in use");
        }
        Person saved = new Person(pid, request.name(), request.grade());
        boolean updated;
        try {
            updated = repository.update(id, saved);
        } catch (DuplicateKeyException e) {
            throw new BadRequestException("name is already in use");
        }
        if (!updated) {
            throw new BadRequestException("no such person");
        }
        audit.record("PERSON_EDITED sheet=" + id + " by=" + by + " name=" + saved.name()
                + " grade=" + saved.grade());
        person.set(saved);
    }
}
