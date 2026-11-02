package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.ConflictException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PathVariable;

/** Save an edited person; refused (409) when someone else has saved them since the editor loaded its version. */
public class SavePerson {
    public void save(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @Val PersonRequest request, ServerHttpConnection connection, PersonRepository repository,
            Audit audit, Rollup rollup, Out<Person> person) throws BadRequestException, ConflictException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        if (request.version() == null) {
            throw new BadRequestException("version is required");
        }
        if (!repository.exists(id, pid)) {
            throw new BadRequestException("no such person");
        }
        if (repository.nameTaken(id, request.name(), pid)) {
            throw new BadRequestException("name is already in use");
        }
        long version = request.version();
        Rollup.Snapshot before = rollup.take(id);
        boolean updated;
        try {
            updated = repository.update(id, pid, version, request.name(), request.grade());
        } catch (DuplicateKeyException e) {
            throw new BadRequestException("name is already in use");
        }
        if (!updated) {
            throw new ConflictException("someone else has saved this person; showing their current values");
        }
        Person saved = repository.find(id, pid);
        audit.record("PERSON_EDITED sheet=" + id + " by=" + by + " name=" + saved.name()
                + " grade=" + saved.grade());
        rollup.record(id, by, before);
        person.set(saved);
    }
}
