package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's pools as they stand once a change is made. */
public class RespondWithPoolsNow {
    public void respond(@Val SheetResponse sheet, PoolRepository repository, ObjectResponse<List<PoolResponse>> response) {
        response.send(repository.list(sheet.id()));
    }
}
