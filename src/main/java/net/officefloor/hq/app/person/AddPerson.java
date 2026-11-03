package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
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
        NewPerson.checkFree(repository, id, request);
        person.set(NewPerson.add(repository, audit, rollup, id, by, request));
    }
}
