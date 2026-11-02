package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.history.AsOfView;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadRates {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            RateRepository repository, AsOfView view, Out<Rates> rates) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        rates.set(view.read(connection, id, () -> new Rates(repository.list(id))));
    }
}
