package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Name another person on the sheet as one of a person's patrons: not themselves, and not one they
 *  have already named. */
public class AddPatron {
    public void add(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @RequestBody PatronRequest request, ServerHttpConnection connection, PersonRepository repository,
            PatronRepository patrons, Audit audit, Out<Person> person) throws BadRequestException {
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
        Person patron = request == null || request.patronId() == null ? null : repository.find(id, request.patronId());
        if (patron == null) {
            throw new BadRequestException("choose a person on the sheet as patron");
        }
        if (patron.id() == pid) {
            throw new BadRequestException("a person cannot be their own patron");
        }
        if (patrons.named(id, pid, patron.id())) {
            throw new BadRequestException(patron.name() + " is already a patron of " + found.name());
        }
        try {
            patrons.add(id, pid, patron.id());
        } catch (DuplicateKeyException e) {
            throw new BadRequestException(patron.name() + " is already a patron of " + found.name());
        }
        audit.record("PATRON_ADDED sheet=" + id + " by=" + by + " name=" + found.name() + " patron=" + patron.name());
        person.set(found);
    }
}
