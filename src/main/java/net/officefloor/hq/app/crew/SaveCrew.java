package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveCrew {
    public void save(@PathVariable("sheetId") String sheetId, @Val CrewInput input, CrewStore store) {
        store.add(sheetId.trim(), input.name(), input.leadId());
    }
}
