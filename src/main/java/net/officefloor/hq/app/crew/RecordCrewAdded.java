package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Writes CREW_ADDED to the audit file once the crew is stored. */
public class RecordCrewAdded {
    public void record(@PathVariable("sheetId") String sheetId, @Val CrewValues values, Audit audit) {
        audit.record("CREW_ADDED sheet=" + sheetId.trim() + " by=" + values.by() + " name=" + values.name()
                + " lead=" + values.lead());
    }
}
