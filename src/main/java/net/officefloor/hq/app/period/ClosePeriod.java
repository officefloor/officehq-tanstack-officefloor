package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes the period (by a role the sheet's close permission gives), fixing its due-total and each person's as they are now (afresh for a soft-closed one); refuses a period already closed. */
public class ClosePeriod {
    public void close(@Val Period period, @Val ChangedBy by, PeriodRepository repository, RoleRepository roles,
            SettingRepository settings, Rollup rollup, Out<ClosedPeriod> closed) throws BadRequestException {
        String role = roles.roleOf(period.sheetId(), by.name());
        if (!settings.allows(period.sheetId(), "close", role)) {
            throw new BadRequestException("A " + role + " may not close a period on this sheet");
        }
        if (period.closed()) {
            throw new BadRequestException("That period is already closed");
        }
        Period fixed = fix(period, repository, rollup);
        if (fixed == null) {
            throw new BadRequestException("The period was closed by someone else; reload the sheet");
        }
        closed.set(new ClosedPeriod(fixed));
    }

    /** Soft-closes the open period, fixing its due-total and each person's as they are now; null when someone else closed or soft-closed it first. */
    static Period softFix(Period period, PeriodRepository repository, Rollup rollup) {
        Map<Long, BigDecimal> personTotals = rollup.dueBetweenByPerson(period.sheetId(), period.from(), period.to());
        BigDecimal total = personTotals.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (!repository.softClose(period.id(), total, personTotals)) {
            return null;
        }
        return new Period(period.id(), period.sheetId(), period.name(), period.from(), period.to(), false, total,
                personTotals, true, period.cap());
    }

    /** Closes the open (or soft-closed) period, fixing its due-total and each person's as they are now; null when someone else closed it first. */
    static Period fix(Period period, PeriodRepository repository, Rollup rollup) {
        Map<Long, BigDecimal> personTotals = rollup.dueBetweenByPerson(period.sheetId(), period.from(), period.to());
        BigDecimal total = personTotals.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (!repository.close(period.id(), total, personTotals)) {
            return null;
        }
        return new Period(period.id(), period.sheetId(), period.name(), period.from(), period.to(), true, total,
                personTotals, false, period.cap());
    }
}
