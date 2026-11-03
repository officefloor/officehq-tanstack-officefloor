package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Set a person's approver: a user name who may approve or reject the person's PENDING claims as a steward may
 *  (replacing any approver before; blank: none). */
public class SetApprover {
    public void set(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @Val ApproverInput input, ServerHttpConnection connection, PersonRepository repository, Audit audit,
            Out<Person> person) throws BadRequestException {
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
        repository.approver(id, pid, input.user().isEmpty() ? null : input.user());
        audit.record("APPROVER_SET sheet=" + id + " by=" + by + " name=" + found.name() + " user=" + input.user());
        person.set(repository.find(id, pid));
    }
}
