package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's pools, in the order added. */
public class LoadPools {
    public void service(@PathVariable("sheetId") String sheetId, Pools pools, ObjectResponse<List<PoolResponse>> response) {
        response.send(pools.list(sheetId));
    }
}
