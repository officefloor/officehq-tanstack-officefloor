package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the crew, with its lead, to the sheet. */
public class SaveCrew {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidCrew crew, Crews crews, Audit audit,
            ObjectResponse<List<CrewResponse>> response) {
        crews.add(sheetId, crew.name(), crew.leadId());
        audit.recordOnCommit("CREW_ADDED sheet=" + sheetId + " by=" + crew.user() + " name=" + crew.name()
                + " lead=" + crew.leadName());
        response.send(crews.list(sheetId));
    }
}
