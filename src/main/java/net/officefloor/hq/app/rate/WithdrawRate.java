package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Withdraws the newest version of a grade's rate for a from-date: the version before it applies again, or the rate is
 * gone when there is none. Only a steward may; refuses anyone else, and a rate not on the sheet.
 */
public class WithdrawRate {
    public void withdraw(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody RateWithdrawRequest request, Rates rates, Roles roles, Audit audit, Rollup rollup)
            throws InvalidRateException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidRateException("Only a steward may withdraw a rate");
        }
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String text = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate from;
        try {
            from = text.isEmpty() || text.equals("start") ? null : LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidRateException("Not a valid from-date: " + text);
        }
        Rollup.Snapshot before = rollup.take(sheet);
        if (!rates.withdraw(sheet, grade, from)) {
            throw new InvalidRateException("There is no rate for grade " + grade + " from that date");
        }
        audit.record("RATE_WITHDRAWN sheet=" + sheet + " by=" + user + " grade=" + grade
                + (from == null ? "" : " from=" + from));
        rollup.record(sheet, user, before);
    }
}
