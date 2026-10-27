package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPool {
    public void respond(@Val Pool pool, ObjectResponse<Pool> response) {
        response.send(pool);
    }
}
