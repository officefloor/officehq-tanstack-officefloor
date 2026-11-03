package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadDescendants {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            SettingsRepository repository, Out<Descendants> descendants) throws BadRequestException {
        CurrentUser.require(connection);
        descendants.set(new Descendants(repository.descendants(sheetId == null ? "" : sheetId.trim())));
    }
}
