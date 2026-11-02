package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Archive the sheet: a steward only, and only once. */
public class ArchiveSheet {
    public void archive(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            SheetRepository repository, SettingsRepository settings, Audit audit, Out<Sheet> sheet)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!settings.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can archive the sheet");
        }
        if (repository.archived(id)) {
            throw new BadRequestException("the sheet is already archived");
        }
        repository.archive(id);
        audit.record("SHEET_ARCHIVED sheet=" + id + " by=" + by);
        sheet.set(new Sheet(id, repository.today(id).toString(), true));
    }
}
