package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRole {
    public void save(@PathVariable("sheetId") String sheetId, @Val Role role, ServerHttpConnection connection,
            SettingsRepository repository, Audit audit, Out<Settings> settings) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        repository.setRole(id, role.user(), role.role());
        audit.record("ROLE_SET sheet=" + id + " by=" + by + " user=" + role.user() + " role=" + role.role());
        settings.set(repository.load(id));
    }
}
