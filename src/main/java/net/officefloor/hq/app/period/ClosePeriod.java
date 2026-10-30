package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Closes a period, fixing its due-total as it is now: the dues of the claims that count dated in
 *  it (a claim whose due is n/a adds nothing). */
public class ClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestBody ClosePeriodRequest request, Periods periods, People people, Claims claims, Rates rates,
            Audit audit, ObjectResponse<List<PeriodResponse>> response) throws InvalidPeriodException {
        PeriodResponse period = periods.lock(sheetId, name);
        if (period == null) {
            throw new InvalidPeriodException("No period named " + name + " is on the sheet");
        }
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + name + " is already closed");
        }
        LocalDate from = LocalDate.parse(period.from());
        LocalDate to = LocalDate.parse(period.to());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        List<Claim> all = claims.list(sheetId);
        for (Person person : people.list(sheetId)) {
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !claim.getStatus().counts()
                        || claim.getDate().isBefore(from) || claim.getDate().isAfter(to)) {
                    continue;
                }
                BigDecimal fixed = ClaimResponse.fixedDue(claim);
                BigDecimal figure = fixed != null || claim.isStated() ? fixed
                        : Rates.due(claim.getAmount(), rates.find(sheetId, person.getGrade(), claim.getDate()));
                if (figure != null) {
                    total = total.add(figure);
                }
            }
        }
        periods.close(sheetId, name, total);
        audit.recordOnCommit("PERIOD_CLOSED sheet=" + sheetId + " by=" + request.user()
                + " period=" + name + " total=" + total.toPlainString());
        response.send(periods.list(sheetId));
    }
}
