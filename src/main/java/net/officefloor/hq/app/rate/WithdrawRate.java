package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Withdraws a rate's newest version, by a steward only: the version before it applies again; with none before it the
 *  rate is gone. */
public class WithdrawRate {
    public void withdraw(@PathVariable("sheetId") String sheetId, @RequestBody WithdrawRateRequest request, Rates rates,
            Settings settings, Audit audit, @Val RollupFigures before, Rollup rollup,
            ObjectResponse<List<RateResponse>> response) throws InvalidRateException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new InvalidRateException("Only a steward may withdraw a rate");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        String rawFrom = request.from() == null ? "" : request.from().trim();
        LocalDate from;
        try {
            from = rawFrom.isEmpty() ? null : LocalDate.parse(rawFrom);
        } catch (DateTimeParseException e) {
            throw new InvalidRateException("The from-date must be a date (YYYY-MM-DD)");
        }
        if (!rates.withdraw(sheetId, grade, from)) {
            throw new InvalidRateException("No rate for grade " + grade + " from " + (from == null ? "the start" : from));
        }
        audit.recordOnCommit("RATE_WITHDRAWN sheet=" + sheetId + " by=" + request.user() + " grade=" + grade
                + (from == null ? "" : " from=" + from));
        rollup.recordChanges(sheetId, request.user(), before);
        response.send(rates.list(sheetId));
    }
}
