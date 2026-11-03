package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Move a person up or down the sheet's list, swapping places with the person above or below them; the
 *  top person cannot move up nor the bottom one down. Writes PERSON_MOVED with their new position (from 1). */
public class MovePerson {
    public void move(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @RequestBody MoveRequest request, ServerHttpConnection connection, PersonRepository repository,
            Audit audit, Out<Person> person) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        String direction = request == null || request.direction() == null ? "" : request.direction().trim();
        if (!direction.equals("up") && !direction.equals("down")) {
            throw new BadRequestException("move up or down");
        }
        List<Person> everyone = repository.list(id);
        int at = -1;
        for (int i = 0; i < everyone.size(); i++) {
            if (everyone.get(i).id() == pid) at = i;
        }
        if (at < 0) {
            throw new BadRequestException("no such person");
        }
        int to = direction.equals("up") ? at - 1 : at + 1;
        if (to < 0) {
            throw new BadRequestException("the top person cannot move up");
        }
        if (to >= everyone.size()) {
            throw new BadRequestException("the bottom person cannot move down");
        }
        Person found = everyone.get(at);
        repository.swapPositions(id, pid, everyone.get(to).id());
        audit.record("PERSON_MOVED sheet=" + id + " by=" + by + " name=" + found.name() + " position=" + (to + 1));
        person.set(found);
    }
}
