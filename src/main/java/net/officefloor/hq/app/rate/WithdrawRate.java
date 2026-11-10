package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A steward withdraws a rate: the newest version of the grade's rate from that from-date is removed and the
 *  version before it, when there is one, applies again; with none before it the rate is gone (RATE_WITHDRAWN and
 *  the rollups it changes). Refused for anyone but a steward, and for a grade and from-date with no rate. Answers
 *  the rate in force from that from-date now, or the withdrawn one with version 0 when none is left. */
public class WithdrawRate {
    public void withdraw(@PathVariable("sheetId") String sheetId, @RequestBody RateWithdrawRequest request,
            ServerHttpConnection connection, RateRepository repository, SettingsRepository settings, Audit audit,
            Rollup rollup, Out<Rate> rate) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String fromText = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!fromText.isEmpty()) {
            try {
                from = LocalDate.parse(fromText);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("from-date must be yyyy-mm-dd");
            }
        }
        if (!SettingsRepository.STEWARD.equals(settings.role(id, by))) {
            throw new BadRequestException("only a steward can withdraw a rate");
        }
        Rollup.Snapshot before = rollup.take(id);
        Rate left = repository.withdraw(id, grade, from);
        if (left == null) {
            throw new BadRequestException("no rate for grade " + grade + (from == null ? "" : " from " + from));
        }
        audit.record("RATE_WITHDRAWN sheet=" + id + " by=" + by + " grade=" + grade
                + (from == null ? "" : " from=" + from));
        rollup.record(id, by, before);
        rate.set(left);
    }
}
