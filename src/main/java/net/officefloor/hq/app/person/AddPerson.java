package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PathVariable;

public class AddPerson {
    public void add(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            ServerHttpConnection connection, PersonRepository repository, Audit audit, Rollup rollup, Out<Person> person)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        if (repository.nameTaken(id, request.name(), 0)) {
            throw new BadRequestException("name is already in use");
        }
        Rollup.Snapshot before = rollup.take(id);
        Person added;
        try {
            added = repository.add(id, request.name(), request.grade());
        } catch (DuplicateKeyException e) {
            throw new BadRequestException("name is already in use");
        }
        audit.record("PERSON_ADDED sheet=" + id + " by=" + by + " name=" + added.name()
                + " grade=" + added.grade());
        rollup.record(id, by, before);
        person.set(added);
    }
}
