package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPool {
    public void respond(@Val Pool pool, ObjectResponse<PoolResponse> response) {
        response.send(PoolResponse.from(pool, List.of(), null));
    }
}
