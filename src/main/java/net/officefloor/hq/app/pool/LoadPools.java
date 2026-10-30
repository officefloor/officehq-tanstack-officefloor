package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools, in the order added; with an as-of date, memberships, holds and the pot judged on it. */
public class LoadPools {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Pools pools,
            ObjectResponse<List<PoolResponse>> response) {
        response.send(pools.list(sheetId, AsOfView.parse(asOf)));
    }
}
