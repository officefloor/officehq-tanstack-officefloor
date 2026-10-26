package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPools {
    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history,
            ObjectResponse<List<PoolResponse>> response) {
        response.send(history.view(sheet.id(), asOf.on(), jdbc -> new PoolRepository(jdbc).list(sheet.id())));
    }
}
