package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.claim.SettleBatches;
import net.officefloor.hq.app.claim.SheetTags;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSheet {
    public void respond(@PathVariable("sheet") String sheet, SheetClock clock, SheetArchive archive, SettleBatches batches,
            SheetTags tags, ObjectResponse<SheetResponse> response) {
        response.send(new SheetResponse(sheet, clock.today(sheet).toString(), archive.archived(sheet),
                batches.last(sheet), tags.list(sheet)));
    }
}
