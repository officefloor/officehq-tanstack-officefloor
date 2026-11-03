package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.history.AsOfView;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadCrews {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            CrewRepository repository, AsOfView view, Out<Crews> crews) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        crews.set(view.read(connection, id, () -> new Crews(repository.list(id))));
    }
}
