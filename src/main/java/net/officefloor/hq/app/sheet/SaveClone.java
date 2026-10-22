package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Clone the sheet to another id when a steward asks and that id was never used (see SheetCloner), recording
 *  SHEET_CLONE_MADE on this sheet, then SHEET_CLONED on the clone followed by a ROLLUP line for every person on it and
 *  its TOTALS. A refused clone writes nothing, not even an audit line. */
public class SaveClone {
    public void save(@PathVariable("sheetId") String sheetId, @Val CloneRequest request, SheetRepository sheets,
            RoleRepository roles, SheetCloner cloner, Rollups rollups, Audit audit, Out<CloneResponse> cloned)
            throws InvalidSheetException {
        String from = sheetId.trim();
        String to = request.toSheet();
        if (!Role.STEWARD.equals(roles.of(from, request.user()))) {
            throw new InvalidSheetException("Only a steward can clone the sheet.");
        }
        if (to.equals(from) || sheets.exists(to)) {
            throw new InvalidSheetException("Sheet " + to + " is already in use: a sheet can only be cloned to an id never used.");
        }
        cloner.copy(from, to, request.user());
        audit.record("SHEET_CLONE_MADE sheet=" + from + " by=" + request.user() + " to=" + to);
        audit.record("SHEET_CLONED sheet=" + to + " by=" + request.user() + " from=" + from);
        Rollup figures = rollups.take(to);
        for (Rollup.Figures f : figures.people().values()) {
            audit.record("ROLLUP sheet=" + to + " by=" + request.user() + " person=" + f.name() + " claimed="
                    + f.claimed() + " due-total=" + f.dueTotal() + " overdue-total=" + f.overdueTotal());
        }
        audit.record("TOTALS sheet=" + to + " by=" + request.user() + " claimed-total=" + figures.claimedTotal()
                + " due-total=" + figures.dueTotal() + " overdue-total=" + figures.overdueTotal());
        cloned.set(new CloneResponse(from, to));
    }
}
