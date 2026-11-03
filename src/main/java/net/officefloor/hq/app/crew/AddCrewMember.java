package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a person to a crew, refusing its lead and a person already in it. */
public class AddCrewMember {
    public void add(@PathVariable("sheetId") String sheetId, @Val CrewMemberRequest input,
            ServerHttpConnection connection, CrewRepository repository, Audit audit) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Long lead = repository.leadOf(id, input.crew());
        if (lead == null) {
            throw new BadRequestException("no such crew");
        }
        String name = repository.personName(id, input.personId());
        if (name == null) {
            throw new BadRequestException("no such person");
        }
        if (lead.longValue() == input.personId()) {
            throw new BadRequestException(name + " leads the crew");
        }
        if (!repository.addMember(id, input.crew(), input.personId())) {
            throw new BadRequestException(name + " is already in the crew");
        }
        audit.record("CREW_MEMBER_ADDED sheet=" + id + " by=" + by + " crew=" + input.crew() + " name=" + name);
    }
}
