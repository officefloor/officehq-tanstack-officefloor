package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A steward clones the sheet to another id, one never used (see {@link SheetCloner#cloneTo}). Refused for anyone
 *  but a steward and for an id already used (this sheet's own included). This sheet writes SHEET_CLONE_MADE; the
 *  clone's first line is SHEET_CLONED, then a rollup line for every person and its totals. */
public class CloneSheet {
    public void cloneSheet(@PathVariable("sheetId") String sheetId, @RequestBody CloneRequest request,
            ServerHttpConnection connection, SettingsRepository settings, SheetCloner cloner, Audit audit, Rollup rollup,
            Out<Clone> clone) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!settings.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can clone the sheet");
        }
        String to = request == null || request.toSheetId() == null ? "" : request.toSheetId().trim();
        if (to.isEmpty()) {
            throw new BadRequestException("enter the sheet to clone to");
        }
        if (settings.sheetExists(to)) {
            throw new BadRequestException("sheet " + to + " is already in use");
        }
        cloner.cloneTo(id, to, by);
        audit.record("SHEET_CLONE_MADE sheet=" + id + " by=" + by + " to=" + to);
        audit.record("SHEET_CLONED sheet=" + to + " by=" + by + " from=" + id);
        rollup.recordAll(to, by);
        clone.set(new Clone(to));
    }
}
