package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class RecordSheetArchived {
    public void record(@PathVariable("sheetId") String sheetId, @Val Actor actor, Audit audit) {
        audit.record("SHEET_ARCHIVED sheet=" + sheetId + " by=" + actor.user());
    }
}
