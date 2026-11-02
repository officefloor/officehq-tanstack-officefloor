package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.history.AsOfView;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadSettings {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            SettingsRepository repository, AsOfView view, Out<Settings> settings) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        settings.set(view.read(connection, id, () -> repository.load(id)));
    }
}
