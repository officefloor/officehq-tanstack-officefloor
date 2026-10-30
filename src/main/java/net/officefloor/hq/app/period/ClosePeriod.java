package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Closes a period, fixing its due-total as it is now: the dues as shown now of the claims dated in it that counted
 *  at the end of its last day (see {@link Person#countsAtEnd}), each with its late charge, and each person's share of it (a person's unbound OPEN claims dated within one of their holds count in none; a claim whose due is n/a adds nothing). */
public class ClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestBody ClosePeriodRequest request, Periods periods, People people, Claims claims, Rates rates,
            Settings settings, Sheets sheets, Audit audit, ObjectResponse<List<PeriodResponse>> response) throws InvalidPeriodException {
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
        LocalDate policyDate = settings.policyDate(sheetId);
        LocalDate today = sheets.today(sheetId);
        List<Claim> all = claims.list(sheetId);
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        for (Person person : people.list(sheetId)) {
            BigDecimal personTotal = BigDecimal.ZERO.setScale(2);
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !person.countsAtEnd(claim, to, today)
                        || claim.getDate().isBefore(from) || claim.getDate().isAfter(to)) {
                    continue;
                }
                RoundingMode rounding = Rounding.of(claim, policyDate);
                BigDecimal fixed = ClaimResponse.fixedDue(claim);
                BigDecimal figure = fixed != null || claim.isBound() ? fixed
                        : Rates.due(claim.getAmount(), rates.find(sheetId, person.gradeOn(claim.getDate()), claim.getDate()),
                        settings.floor(sheetId, claim.getDate()), rounding);
                if (figure != null) {
                    personTotal = personTotal.add(LateCharge.withCharge(claim, figure, settings.latePct(sheetId, claim.getDate()), today, rounding));
                }
            }
            personTotals.put(person.getId(), personTotal);
            total = total.add(personTotal);
        }
        periods.close(sheetId, name, total, personTotals);
        audit.recordOnCommit("PERIOD_CLOSED sheet=" + sheetId + " by=" + request.user()
                + " period=" + name + " total=" + total.toPlainString());
        response.send(periods.list(sheetId));
    }
}
