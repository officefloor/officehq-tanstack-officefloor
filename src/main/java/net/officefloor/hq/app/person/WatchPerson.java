package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Start or stop watching a person as the signed-in user (per user; it changes nothing on the sheet itself; a viewer is refused).
 *  Asking for the state already held writes nothing. */
public class WatchPerson {
    public void watch(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @RequestBody WatchRequest request, ServerHttpConnection connection, PersonRepository repository,
            WatchRepository watches, Audit audit, Out<Person> person) throws BadRequestException {
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
        if (request == null || request.watch() == null) {
            throw new BadRequestException("say whether to watch the person");
        }
        boolean on = request.watch();
        if (on != watches.watches(id, pid, by)) {
            if (on) {
                watches.watch(id, pid, by);
            } else {
                watches.unwatch(id, pid, by);
            }
            audit.record((on ? "PERSON_WATCHED" : "PERSON_UNWATCHED") + " sheet=" + id + " by=" + by + " name=" + found.name());
        }
        person.set(found);
    }
}
