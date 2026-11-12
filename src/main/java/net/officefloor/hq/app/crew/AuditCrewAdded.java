package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a crew added: CREW_ADDED sheet=<id> by=<user> name=<name> lead=<person>. */
public class AuditCrewAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val CrewInput input, Audit audit) {
        audit.record("CREW_ADDED sheet=" + sheetId.trim() + " by=" + input.user() + " name=" + input.name()
                + " lead=" + input.leadName());
    }
}
