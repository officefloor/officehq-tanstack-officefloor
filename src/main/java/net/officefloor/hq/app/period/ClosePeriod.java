package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.Rates;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Closes the period, fixing its due-total now: the due of the counted (OPEN and SETTLED) claims dated in it (a
 * claim with no rate adds nothing). Refuses a period not on the sheet or already closed.
 */
public class ClosePeriod {
    public void close(@PathVariable("sheet") String sheet, @PathVariable("period") String period,
            @RequestParam("user") String user, Periods periods, Claims claims, People people, Rates rates, Audit audit)
            throws InvalidPeriodException {
        Period found = periods.find(sheet, period);
        if (found == null) {
            throw new InvalidPeriodException("There is no period named " + period);
        }
        if (found.closed()) {
            throw new InvalidPeriodException("The period " + period + " is already closed");
        }
        LocalDate from = LocalDate.parse(found.from());
        LocalDate to = LocalDate.parse(found.to());
        Map<Long, String> grades = people.list(sheet).stream().collect(Collectors.toMap(Person::id, Person::grade));
        List<Rate> sheetRates = rates.list(sheet);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim claim : claims.list(sheet)) {
            LocalDate date = LocalDate.parse(claim.date());
            String grade = grades.get(claim.person());
            if (!claim.counts() || grade == null || date.isBefore(from) || date.isAfter(to)) {
                continue;
            }
            BigDecimal due = claim.due(sheetRates, grade);
            total = due == null ? total : total.add(due);
        }
        if (!periods.close(sheet, period, total)) {
            throw new InvalidPeriodException("The period " + period + " is already closed");
        }
        audit.record("PERIOD_CLOSED sheet=" + sheet + " by=" + user + " period=" + period + " total="
                + total.toPlainString());
    }
}
