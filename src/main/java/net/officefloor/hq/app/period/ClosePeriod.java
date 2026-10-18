package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Val;

/** Closes the period, refusing one already closed, and fixes its due-total: the due of the claims that
 *  count dated in it (not the OPEN claims of a person on hold), as shown now (a settled claim's fixed due; an open one's at the rate in force on its
 *  date, rounded half to even), none where no rate applies. */
public class ClosePeriod {
    public void close(@Val Period period, ClaimRepository claims, PersonRepository people, RateRepository rates)
            throws InvalidPeriodException {
        if (period.isClosed()) {
            throw new InvalidPeriodException("The period " + period.getName() + " is already closed.");
        }
        Map<Long, Person> byId = people.findBySheetIdOrderByIdAsc(period.getSheetId()).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim c : claims.findBySheetIdOrderByIdDesc(period.getSheetId())) {
            if (!c.getStatus().counts() || !period.contains(c.getDate())
                    || (c.getStatus() == ClaimStatus.OPEN && byId.get(c.getPersonId()).isHeld())) {
                continue;
            }
            BigDecimal due = c.getStatus() == ClaimStatus.SETTLED ? c.getSettledDue()
                    : rates.findInForce(c.getSheetId(), byId.get(c.getPersonId()).getGrade(), c.getDate())
                            .map((r) -> c.getAmount().multiply(r.getRate())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN))
                            .orElse(null);
            if (due != null) {
                total = total.add(due);
            }
        }
        period.close(total);
    }
}
