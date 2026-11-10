package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Closes the period, fixing its due-total at this moment: the figures of the claims that counted at
 * the end of its last day, by the status their log records then (OPEN, PENDING and SETTLED, but not an
 * unbound open one dated within a hold of its person, nor one deferred now), due by a date inside it (an overdue one with its late charge), added up, skipping n/a. Each person's own figure for the period is fixed
 * too, and the period's due-total is the sum of them. A soft-closed period closes too, its figures
 * fixed afresh. A period already closed is refused, and so is a role the permission matrix does not allow to close.
 */
public class ClosePeriod {
    public void close(@Val Sheet sheet, @Val Period period, PeriodRepository periods, PersonRepository people,
            ClaimRepository claims, ClaimLogRepository log, RateRepository rates, SettingsRepository settings, RoleRepository roles,
            HttpServletRequest request, Out<Period> closedOut) throws InvalidPeriodException {
        String role = roles.roleOf(sheet.id(), Audit.user(request));
        if (!settings.find(sheet.id()).allows("close", role)) {
            throw new InvalidPeriodException("A " + role + " may not close period " + period.name());
        }
        if (period.hardClosed()) throw new InvalidPeriodException("Period " + period.name() + " is already closed");
        Period closed = fix(sheet, period, false, periods, people, claims, log, rates, settings);
        if (closed == null) throw new InvalidPeriodException("Period " + period.name() + " is already closed");
        closedOut.set(closed);
    }

    /**
     * Fixes the open (or soft-closed) period's due-total, and each person's, as at the sheet's today,
     * and marks it closed, or soft-closed when {@code soft}, in the store: the closed period, or null
     * when another request changed it first.
     */
    static Period fix(Sheet sheet, Period period, boolean soft, PeriodRepository periods, PersonRepository people,
            ClaimRepository claims, ClaimLogRepository log, RateRepository rates, SettingsRepository settings) {
        List<Rate> all = rates.findAll(sheet.id());
        Settings sheetSettings = settings.find(sheet.id());
        List<Claim> sheetClaims = claims.findAll(sheet.id());
        Map<Long, ClaimStatus> statusesThen = log.statusesOn(sheet.id(), period.to());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        for (Person person : people.findAll(sheet.id())) {
            BigDecimal personTotal = BigDecimal.ZERO.setScale(2);
            for (Claim claim : sheetClaims) {
                if (claim.getPersonId() != person.getId() || !period.contains(claim.getDueBy())) continue;
                if (!claim.countsAsOf(statusesThen.get(claim.getId()), person.heldOn(claim.getDate(), sheet.today()), sheet.today())) continue;
                BigDecimal figure = ClaimFigure.withLate(claim, all, person.gradeOn(claim.getDate()), sheetSettings, sheet.today());
                if (figure != null) personTotal = personTotal.add(figure);
            }
            personTotals.put(person.getId(), personTotal);
            total = total.add(personTotal);
        }
        Period closed = soft ? period.softClose(total) : period.close(total);
        if (!periods.markClosed(closed, period.soft())) return null;
        periods.deletePersonTotals(closed);
        periods.savePersonTotals(closed, personTotals);
        return closed;
    }
}
