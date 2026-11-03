package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Give a person an alias (trimmed; required): it must not equal, ignoring case, any name or alias on the sheet
 *  or on another sheet of its tree. */
public class AddAlias {
    public void add(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @RequestBody AliasRequest request, ServerHttpConnection connection, PersonRepository repository,
            AliasRepository aliases, Audit audit, Out<Person> person) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person found = repository.find(id, pid);
        if (found == null) {
            throw new BadRequestException("no such person");
        }
        String alias = request == null || request.alias() == null ? "" : request.alias().trim();
        if (alias.isEmpty()) {
            throw new BadRequestException("alias is required");
        }
        if (repository.nameTaken(id, alias, 0)) {
            throw new BadRequestException("name is already in use");
        }
        aliases.add(id, pid, alias);
        audit.record("PERSON_ALIAS_ADDED sheet=" + id + " by=" + by + " name=" + found.name() + " alias=" + alias);
        person.set(found);
    }
}
