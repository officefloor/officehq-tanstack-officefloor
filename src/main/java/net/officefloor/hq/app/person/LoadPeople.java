package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPeople {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            PersonRepository repository, PatronRepository patrons, WatchRepository watches, Out<People> people) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        people.set(new People(repository.list(id), patrons.list(id), watches.watched(id, by)));
    }
}
