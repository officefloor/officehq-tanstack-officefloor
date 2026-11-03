package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a reason to the sheet's void reasons; one already on the list is refused. */
public class SaveVoidReason {
    public void save(@PathVariable("sheetId") String sheetId, @Val String voidReason, ServerHttpConnection connection,
            SettingsRepository repository, Audit audit, Out<Settings> settings) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        if (repository.voidReasons(id).contains(voidReason)) {
            throw new BadRequestException(voidReason + " is already a void reason");
        }
        repository.addVoidReason(id, voidReason);
        audit.record("VOID_REASON_ADDED sheet=" + id + " by=" + by + " reason=" + voidReason);
        settings.set(repository.load(id));
    }
}
