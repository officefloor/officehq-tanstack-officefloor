package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with every pool on the sheet. */
public class RespondWithPools {
    public void respond(@PathVariable("sheet") String sheet, Pools pools, ObjectResponse<List<Pool>> response) {
        response.send(pools.list(sheet));
    }
}
