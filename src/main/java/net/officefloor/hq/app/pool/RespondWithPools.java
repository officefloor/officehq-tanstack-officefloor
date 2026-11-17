package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.Preview;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools in the order added, each with its members, total share and whether it is balanced. Read as of a
 *  date (see AsOf), the date stands in for the sheet's today. */
public class RespondWithPools {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf,
            @RequestParam(value = "preview", required = false) String preview, PoolRepository pools,
            ObjectResponse<List<Pool>> response) {
        response.send(pools.list(sheetId.trim(), AsOf.parse(asOf), Preview.parse(preview)));
    }
}
