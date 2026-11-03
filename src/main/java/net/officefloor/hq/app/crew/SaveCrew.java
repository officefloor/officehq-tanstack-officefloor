package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a crew, refusing a name the sheet already has or a lead not on the sheet. */
public class SaveCrew {
    public void save(@PathVariable("sheetId") String sheetId, @Val CrewRequest input, ServerHttpConnection connection,
            CrewRepository repository, Audit audit) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        String lead = repository.personName(id, input.leadId());
        if (lead == null) {
            throw new BadRequestException("no such person");
        }
        if (!repository.add(id, input.name(), input.leadId())) {
            throw new BadRequestException("a crew of that name already exists");
        }
        audit.record("CREW_ADDED sheet=" + id + " by=" + by + " name=" + input.name() + " lead=" + lead);
    }
}
