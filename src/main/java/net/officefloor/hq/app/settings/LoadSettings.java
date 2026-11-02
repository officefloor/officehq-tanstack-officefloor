package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadSettings {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            SettingsRepository repository, Out<Settings> settings) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        settings.set(repository.load(id));
    }
}
