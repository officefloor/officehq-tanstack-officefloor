package net.officefloor.hq.app.period;

import java.sql.Date;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.opening.Openings;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Undoes the sheet's last rollover: by the steward who made it, on the sheet's today it was made, and only while no
 * claim (of any status) belongs to a period it generated. The periods it generated are removed and the openings it
 * set taken back, then ROLLOVER_UNDONE with the old fiscal year.
 */
public class UndoRollover {
    public void undo(@PathVariable("sheetId") String sheetId, @RequestBody RolloverUndoRequest request,
            Rollovers rollovers, Periods periods, Openings openings, Settings settings, Sheets sheets,
            JdbcTemplate jdbc, Audit audit, ObjectResponse<GenerateResponse> response)
            throws InvalidPeriodException, ViewerRefusedException {
        String user = request.user();
        settings.refuseViewer(sheetId, user);
        if (!Settings.STEWARD.equals(settings.role(sheetId, user))) {
            throw new InvalidPeriodException("Only a steward may undo a rollover");
        }
        Rollovers.Rollover last = rollovers.last(sheetId);
        if (last == null || last.undone()) {
            throw new InvalidPeriodException("There is no rollover to undo");
        }
        if (!last.user().equals(user)) {
            throw new InvalidPeriodException("Only the steward who rolled the year over may undo it");
        }
        if (!last.madeOn().equals(sheets.today(sheetId))) {
            throw new InvalidPeriodException("A rollover can be undone only on the day it was made");
        }
        for (String name : last.periods()) {
            PeriodResponse period = periods.lock(sheetId, name);
            if (period == null) {
                continue;
            }
            Integer claimed = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND claim_date >= ? AND claim_date <= ?", Integer.class,
                    sheetId, Date.valueOf(period.from()), Date.valueOf(period.to()));
            if (claimed != null && claimed > 0) {
                throw new InvalidPeriodException("A claim belongs to period " + name + " the rollover generated");
            }
        }
        last.periods().forEach(name -> periods.remove(sheetId, name));
        openings.removeByRollover(sheetId, last.id());
        rollovers.undone(last.id());
        audit.recordOnCommit("ROLLOVER_UNDONE sheet=" + sheetId + " by=" + user + " fy=" + last.fiscalYear());
        response.send(new GenerateResponse(last.periods().size(), periods.list(sheetId)));
    }
}
