package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Closes the period, fixing its due-total at this moment: the figures of the claims that count (OPEN
 * and SETTLED, but not an unbound OPEN one dated within a hold of its person) dated inside it (an overdue one with its late charge), added up, skipping n/a. Each person's own figure for the period is fixed
 * too, and the period's due-total is the sum of them. A period already closed is refused.
 */
public class ClosePeriod {
    public void close(@Val Sheet sheet, @Val Period period, PeriodRepository periods, PersonRepository people,
            ClaimRepository claims, RateRepository rates, SettingsRepository settings, Out<Period> closedOut) throws InvalidPeriodException {
        if (period.closed()) throw new InvalidPeriodException("Period " + period.name() + " is already closed");
        List<Rate> all = rates.findAll(sheet.id());
        Settings sheetSettings = settings.find(sheet.id());
        List<Claim> sheetClaims = claims.findAll(sheet.id());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        for (Person person : people.findAll(sheet.id())) {
            BigDecimal personTotal = BigDecimal.ZERO.setScale(2);
            for (Claim claim : sheetClaims) {
                if (claim.getPersonId() != person.getId() || !period.contains(claim.getDate())) continue;
                if (!claim.counts(person.heldOn(claim.getDate()))) continue;
                BigDecimal figure = ClaimFigure.withLate(claim, all, person.gradeOn(claim.getDate()), sheetSettings, sheet.today());
                if (figure != null) personTotal = personTotal.add(figure);
            }
            personTotals.put(person.getId(), personTotal);
            total = total.add(personTotal);
        }
        Period closed = period.close(total);
        if (!periods.markClosed(closed)) {
            throw new InvalidPeriodException("Period " + period.name() + " is already closed");
        }
        periods.savePersonTotals(closed, personTotals);
        closedOut.set(closed);
    }
}
