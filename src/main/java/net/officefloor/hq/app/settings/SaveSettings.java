package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveSettings {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingsInput input,
            ServerHttpConnection connection, SettingsRepository repository, Audit audit, Out<Settings> settings)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        repository.setClerkLimit(id, input.clerkLimit());
        audit.record("SETTING_CHANGED sheet=" + id + " by=" + by + " key=clerk-limit value="
                + (input.clerkLimit() == null ? "" : SettingsRepository.money(input.clerkLimit())));
        settings.set(repository.load(id));
    }
}
