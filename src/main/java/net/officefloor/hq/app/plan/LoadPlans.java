package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.history.AsOfView;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPlans {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            PlanRepository repository, AsOfView view, Out<Plans> plans) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        plans.set(view.read(connection, id, () -> new Plans(repository.list(id))));
    }
}
