package net.officefloor.hq.app.period;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.history.AsOfView;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPeriods {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            PeriodRepository repository, AsOfView view, Out<Periods> periods) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        periods.set(view.read(connection, id, () -> new Periods(repository.list(id))));
    }
}
