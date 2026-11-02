package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadRates {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            RateRepository repository, Out<Rates> rates) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        rates.set(new Rates(repository.list(id)));
    }
}
