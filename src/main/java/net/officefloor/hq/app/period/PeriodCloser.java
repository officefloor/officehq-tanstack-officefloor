package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Dated;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;

/**
 * Closes a period, fixing its due-total now, and each person's on the sheet (the part of it from their claims): the due now of the claims whose due-by falls in it
 * that counted at the end of its last day (OPEN, PENDING and SETTLED then by their status log, but not a deferred one, nor an unbound open one dated
 * within its person's hold) (a claim with no rate adds nothing); and each person's carried figure for it: the due now of
 * their claims that count at its end whose due-by falls in an earlier period (one ending before it starts) and that were
 * still OPEN or PENDING at its end by their status log. Shared by a close by hand and the auto close of a today change.
 */
@Service
public class PeriodCloser {

    private final Periods periods;
    private final Claims claims;
    private final People people;
    private final Rates rates;
    private final Settings settings;
    private final SheetClock clock;
    private final Audit audit;

    public PeriodCloser(Periods periods, Claims claims, People people, Rates rates, Settings settings, SheetClock clock,
            Audit audit) {
        this.periods = periods;
        this.claims = claims;
        this.people = people;
        this.rates = rates;
        this.settings = settings;
        this.clock = clock;
        this.audit = audit;
    }

    /** Closes the period found open on the sheet; false when it was already closed. An auto close is audited auto=yes. */
    public boolean close(String sheet, String user, Period found, boolean auto) {
        return close(sheet, user, found, auto, false);
    }

    /**
     * Closes the period found open (or soft-closed) on the sheet, or soft-closes it, fixing its figures afresh; false
     * when it was already closed, or for a soft close already soft-closed.
     */
    public boolean close(String sheet, String user, Period found, boolean auto, boolean soft) {
        LocalDate from = LocalDate.parse(found.from());
        LocalDate to = LocalDate.parse(found.to());
        Map<Long, Person> byId = people.list(sheet).stream().collect(Collectors.toMap(Person::id, (p) -> p));
        List<Rate> sheetRates = rates.list(sheet);
        Dated floor = settings.floor(sheet);
        Late late = settings.late(sheet, clock.today(sheet));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> perPerson = new LinkedHashMap<>();
        byId.keySet().forEach((id) -> perPerson.put(id, BigDecimal.ZERO.setScale(2)));
        Map<Long, BigDecimal> carried = new LinkedHashMap<>(perPerson);
        List<Period> sheetPeriods = periods.list(sheet);
        List<Claim> sheetClaims = claims.list(sheet);
        for (Claim claim : sheetClaims) {
            Person person = byId.get(claim.person());
            LocalDate dueBy = LocalDate.parse(claim.dueBy());
            boolean earlier = sheetPeriods.stream().anyMatch((p) -> !dueBy.isBefore(LocalDate.parse(p.from()))
                    && !dueBy.isAfter(LocalDate.parse(p.to())) && LocalDate.parse(p.to()).isBefore(from));
            ClaimStatus then = claim.statusOn(to);
            if (person == null || !earlier || (then != ClaimStatus.OPEN && then != ClaimStatus.PENDING)
                    || !person.countsOn(claim, to)) {
                continue;
            }
            BigDecimal due = claim.due(sheetRates, person.gradeOn(claim.date()), floor, late);
            if (due != null) {
                carried.merge(person.id(), due, BigDecimal::add);
            }
        }
        for (Claim claim : sheetClaims) {
            LocalDate date = LocalDate.parse(claim.dueBy());
            Person person = byId.get(claim.person());
            if (person == null || !person.countsOn(claim, to) || date.isBefore(from) || date.isAfter(to)) {
                continue;
            }
            BigDecimal due = claim.due(sheetRates, person.gradeOn(claim.date()), floor, late);
            if (due != null) {
                total = total.add(due);
                perPerson.merge(person.id(), due, BigDecimal::add);
            }
        }
        if (!periods.close(sheet, found.name(), total, perPerson, carried, soft)) {
            return false;
        }
        audit.record((soft ? "PERIOD_SOFT_CLOSED" : "PERIOD_CLOSED") + " sheet=" + sheet + " by=" + user + " period=" + found.name() + " total="
                + total.toPlainString() + (auto ? " auto=yes" : ""));
        return true;
    }

    /**
     * Closes, in order of their to-dates, every open (neither closed nor soft-closed) period of the sheet whose to-date plus the grace days is before
     * the sheet's today; none when the sheet has no grace days.
     */
    public void autoClose(String sheet, String user) {
        Integer grace = settings.graceDays(sheet);
        if (grace == null) {
            return;
        }
        LocalDate today = clock.today(sheet);
        List<Period> due = periods.list(sheet).stream()
                .filter((p) -> !p.fixed() && LocalDate.parse(p.to()).plusDays(grace).isBefore(today))
                .sorted(Comparator.comparing(Period::to)).toList();
        for (Period period : due) {
            close(sheet, user, period, true);
        }
    }
}
