package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's pools in the order added, each with its members, total share and whether it is balanced. */
public class RespondWithPools {
    public void respond(@PathVariable("sheetId") String sheetId, PoolRepository pools,
            ObjectResponse<List<Pool>> response) {
        response.send(pools.list(sheetId.trim()));
    }
}
