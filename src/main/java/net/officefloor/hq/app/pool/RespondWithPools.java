package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Answers a change with the sheet's pools as they now stand. */
public class RespondWithPools {
    public void respond(@PathVariable("sheetId") String sheetId, PoolRepository pools,
            ObjectResponse<List<Pool>> response) {
        response.send(pools.list(sheetId.trim()));
    }
}
