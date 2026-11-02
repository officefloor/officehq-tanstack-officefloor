package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class AddPerson {
    public void add(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            ServerHttpConnection connection, PersonRepository repository, Audit audit, Out<Person> person)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        Person added = repository.add(id, request.name(), request.grade());
        audit.record("person-add sheet=" + id + " by=" + by + " person=" + added.id()
                + " grade=" + added.grade() + " name=" + added.name());
        person.set(added);
    }
}
