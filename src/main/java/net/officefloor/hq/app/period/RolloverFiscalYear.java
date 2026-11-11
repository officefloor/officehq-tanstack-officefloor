package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.FrozenCuts;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Rolls fiscal year FY<yyyy> over. Only a steward may; a year with no periods on the sheet is refused.
 * Makes the next fiscal year's twelve monthly periods exactly as the fiscal year generator does; then,
 * for each person, their open figure at the end of the old year's last period (the figures of their
 * claims that count, belonging by due-by to a period of the old year, and OPEN or PENDING at the end of
 * that last period's last day by the status their log records then, added up, skipping n/a) is added to
 * their opening balance for the new year's first period (OPENING_SET; none for a figure of 0.00).
 * A year rolled over again while an earlier rollover of it stands carries no openings a second time. Last,
 * ROLLOVER with the old year and how many periods were made. The rollover is kept, with the periods it
 * made and the openings it added, so it can be undone (see {@link UndoRollover}). After ROLLOVER, every
 * period of the old year still open is soft-closed, in order of their from-dates, exactly as a soft close
 * by hand would, each recording PERIOD_SOFT_CLOSED.
 */
public class RolloverFiscalYear {
    public void rollover(@Val FiscalYear fiscalYear, @Val Sheet sheet, SettingsRepository settings,
            PeriodRepository periods, PersonRepository people, ClaimRepository claims, ClaimLogRepository log,
            RateRepository rates, RoleRepository roles, RolloverRepository rollovers, FrozenCuts frozen, HttpServletRequest request, Audit audit,
            Out<GeneratePeriodsResponse> result) throws StewardOnlyException, InvalidPeriodException {
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new StewardOnlyException("Only a steward may roll the year over");
        }
        Settings sheetSettings = settings.find(sheet.id());
        String oldFy = "FY" + fiscalYear.year();
        String newFy = "FY" + (fiscalYear.year() + 1);
        List<Period> old = periods.findAll(sheet.id()).stream()
                .filter((p) -> oldFy.equals(sheetSettings.fiscalYearOf(p.from()))).toList();
        if (old.isEmpty()) throw new InvalidPeriodException(oldFy + " has no periods");
        Period last = old.stream().max(Comparator.comparing(Period::to)).get();

        boolean carried = rollovers.stands(sheet.id(), oldFy);
        List<String> before = periods.findAll(sheet.id()).stream().map(Period::name).toList();
        LocalDate from = LocalDate.of(fiscalYear.year() + 1, sheetSettings.fiscalStart(), 1);
        int made = GeneratePeriods.months(from, from.plusMonths(12).minusDays(1), sheet, periods, request, audit);

        Period first = periods.findAll(sheet.id()).stream()
                .filter((p) -> newFy.equals(sheetSettings.fiscalYearOf(p.from()))).findFirst().orElse(null);
        List<String> generated = periods.findAll(sheet.id()).stream().map(Period::name)
                .filter((n) -> !before.contains(n)).toList();
        List<Rollover.Opening> openings = new ArrayList<>();
        if (first != null && !carried) {
            List<Rate> all = rates.findAll(sheet.id());
            List<Claim> sheetClaims = claims.findAll(sheet.id());
            Map<Long, ClaimStatus> statusesThen = log.statusesOn(sheet.id(), last.to());
            for (Person person : people.findAll(sheet.id())) {
                BigDecimal open = BigDecimal.ZERO.setScale(2);
                for (Claim claim : sheetClaims) {
                    if (claim.getPersonId() != person.getId() || claim.getDueBy() == null) continue;
                    if (old.stream().noneMatch((p) -> p.contains(claim.getDueBy()))) continue;
                    ClaimStatus then = statusesThen.get(claim.getId());
                    if (then != ClaimStatus.OPEN && then != ClaimStatus.PENDING) continue;
                    if (!claim.countsAsOf(then, person.heldOn(claim.getDate(), sheet.today()), sheet.today())) continue;
                    BigDecimal figure = ClaimFigure.withLate(claim, all, person.gradeOn(claim.getDate()), sheetSettings, sheet.today());
                    if (figure != null) open = open.add(figure);
                }
                if (open.signum() == 0) continue;
                periods.addOpening(sheet.id(), first.name(), person.getId(), open);
                openings.add(new Rollover.Opening(first.name(), person.getId(), open));
                audit.record("OPENING_SET sheet=" + sheet.id() + " by=" + user + " name=" + person.getName()
                        + " period=" + first.name() + " amount=" + open.setScale(2).toPlainString());
            }
        }
        rollovers.insert(sheet.id(), oldFy, user, sheet.today(), generated, openings);
        audit.record("ROLLOVER sheet=" + sheet.id() + " by=" + user + " fy=" + oldFy + " periods=" + made);
        List<Period> stillOpen = periods.findAll(sheet.id()).stream()
                .filter((p) -> !p.closed() && oldFy.equals(sheetSettings.fiscalYearOf(p.from())))
                .sorted(Comparator.comparing(Period::from)).toList();
        for (Period period : stillOpen) {
            Period closed = ClosePeriod.fix(sheet, period, true, periods, people, claims, log, rates, settings, frozen);
            if (closed == null) continue; // another request closed it first
            audit.record("PERIOD_SOFT_CLOSED sheet=" + sheet.id() + " by=" + user + " period=" + closed.name()
                    + " total=" + closed.closedTotal().setScale(2).toPlainString());
        }
        result.set(new GeneratePeriodsResponse(made));
    }
}
