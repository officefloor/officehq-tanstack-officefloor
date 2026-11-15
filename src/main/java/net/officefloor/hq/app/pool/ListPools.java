package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListPools {
    public void list(@PathVariable("sheetId") String sheetId, PoolRepository pools,
            ObjectResponse<List<Pool>> response) {
        response.send(pools.list(sheetId.trim()));
    }
}
