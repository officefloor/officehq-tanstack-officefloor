package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;

/** Soft-closing a period on its sheet, shared by the soft close and the rollover. */
public final class SoftCloses {
    private SoftCloses() {
    }

    /** Soft-closes the period, fixing each person's due-total and carried figure for it, each pool member's cut for it,
     *  and its own due-total exactly as closing does, and saves it. */
    public static void fix(Period period, PeriodRepository repository, Sheet sheet, ClaimRepository claims,
            PersonRepository people, RateRepository rates, PersonGradeRepository grades, ClaimLogRepository log,
            AsOf asOf) {
        period.softClose(ClosePeriod.totals(period, sheet, claims, people, rates, grades, log),
                ClosePeriod.carried(period, repository, sheet, claims, people, rates, grades, log));
        period.fixPoolCuts(PoolCuts.live(sheet, asOf));
        repository.saveAndFlush(period);
    }
}
