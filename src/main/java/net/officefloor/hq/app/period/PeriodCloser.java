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
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.opening.Openings;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.stereotype.Service;

/** Closes a period, fixing its due-total as it is now: the dues as shown now of the claims whose due-by is in it that counted
 *  at the end of its last day (see {@link Person#countsAtEnd}), each with its late charge, and each person's share of it (a hold takes no claim out; a claim whose due is n/a adds nothing).
 *  Used by a close by hand and by the auto close when the sheet's today is changed (see {@link #closeDue}). */
@Service
public class PeriodCloser {

    private final Periods periods;
    private final People people;
    private final Claims claims;
    private final Rates rates;
    private final Settings settings;
    private final Sheets sheets;
    private final Audit audit;
    private final Openings openings;

    public PeriodCloser(Periods periods, People people, Claims claims, Rates rates, Settings settings, Sheets sheets,
            Audit audit, Openings openings) {
        this.openings = openings;
        this.periods = periods;
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.settings = settings;
        this.sheets = sheets;
        this.audit = audit;
    }

    /** Close the open (or soft-closed) period, auditing PERIOD_CLOSED (with auto=yes when closed by the auto close). */
    public void close(String sheetId, PeriodResponse period, String user, boolean auto) {
        close(sheetId, period, user, auto, false);
    }

    /** Soft-close the open period: its figures are fixed as by a close, but its claims stay open to change.
     *  Audits PERIOD_SOFT_CLOSED. */
    public void softClose(String sheetId, PeriodResponse period, String user) {
        close(sheetId, period, user, false, true);
    }

    private void close(String sheetId, PeriodResponse period, String user, boolean auto, boolean soft) {
        LocalDate from = LocalDate.parse(period.from());
        LocalDate to = LocalDate.parse(period.to());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        LocalDate policyDate = settings.policyDate(sheetId);
        LocalDate today = sheets.today(sheetId);
        List<Claim> all = claims.list(sheetId);
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        Map<Long, BigDecimal> personCarried = new LinkedHashMap<>();
        List<PeriodResponse> allPeriods = periods.list(sheetId);
        Map<Long, BigDecimal> opening = openings.forPeriod(sheetId, period.name());
        for (Person person : people.list(sheetId)) {
            personCarried.put(person.getId(), carried(sheetId, person, all, allPeriods, from, to, policyDate, today)
                    .add(opening.getOrDefault(person.getId(), BigDecimal.ZERO.setScale(2))));
            BigDecimal personTotal = BigDecimal.ZERO.setScale(2);
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !person.countsAtEnd(claim, to, today)
                        || claim.getPeriodDate().isBefore(from) || claim.getPeriodDate().isAfter(to)) {
                    continue;
                }
                personTotal = personTotal.add(due(sheetId, person, claim, policyDate, today));
            }
            personTotals.put(person.getId(), personTotal);
            total = total.add(personTotal);
        }
        periods.close(sheetId, period.name(), total, personTotals, personCarried, soft);
        audit.recordOnCommit((soft ? "PERIOD_SOFT_CLOSED" : "PERIOD_CLOSED") + " sheet=" + sheetId + " by=" + user
                + " period=" + period.name() + " total=" + total.toPlainString() + (auto ? " auto=yes" : ""));
    }

    /** A claim's due as shown now, its late charge included; 0.00 when it is n/a. */
    private BigDecimal due(String sheetId, Person person, Claim claim, LocalDate policyDate, LocalDate today) {
        RoundingMode rounding = Rounding.of(claim, policyDate, on -> settings.roundingMode(sheetId, on));
        BigDecimal fixed = ClaimResponse.fixedDue(claim);
        BigDecimal figure = fixed != null || claim.isBound() ? fixed
                : Rates.due(claim.getAmount(), rates.find(claim, person.gradeOn(claim.getDate())), rounding);
        if (figure == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return LateCharge.due(claim, figure, settings.floor(sheetId, claim.getDate()), on -> settings.latePct(sheetId, on),
                today, rounding, settings.grace(sheetId, person.gradeOn(claim.getDate()), claim.getDate()));
    }

    /** A person's carried figure for the period from-to: the dues as shown now of their claims whose due-by falls in a
     *  period ending before this one starts that counted at the end of its last day and were then still OPEN or PENDING
     *  (the person's opening balance for the period is added to it by the close). */
    private BigDecimal carried(String sheetId, Person person, List<Claim> all, List<PeriodResponse> allPeriods,
            LocalDate from, LocalDate to, LocalDate policyDate, LocalDate today) {
        BigDecimal carried = BigDecimal.ZERO.setScale(2);
        for (Claim claim : all) {
            if (claim.getPersonId() != person.getId()) {
                continue;
            }
            LocalDate dueBy = claim.getPeriodDate();
            boolean earlier = allPeriods.stream().anyMatch(p -> !LocalDate.parse(p.from()).isAfter(dueBy)
                    && !LocalDate.parse(p.to()).isBefore(dueBy) && LocalDate.parse(p.to()).isBefore(from));
            ClaimStatus then = claim.statusAt(to);
            if (!earlier || (then != ClaimStatus.OPEN && then != ClaimStatus.PENDING) || !person.countsAtEnd(claim, to, today)) {
                continue;
            }
            carried = carried.add(due(sheetId, person, claim, policyDate, today));
        }
        return carried;
    }

    /** A person's open figure at the end date: the dues as shown now of their claims whose due-by falls in one of the
     *  periods that counted at the end date (see {@link Person#countsAtEnd}) and were then still OPEN or PENDING. */
    public BigDecimal openAt(String sheetId, Person person, List<Claim> all, List<PeriodResponse> in, LocalDate end) {
        LocalDate policyDate = settings.policyDate(sheetId);
        LocalDate today = sheets.today(sheetId);
        BigDecimal open = BigDecimal.ZERO.setScale(2);
        for (Claim claim : all) {
            if (claim.getPersonId() != person.getId()) {
                continue;
            }
            LocalDate dueBy = claim.getPeriodDate();
            boolean inside = in.stream().anyMatch(p -> !LocalDate.parse(p.from()).isAfter(dueBy)
                    && !LocalDate.parse(p.to()).isBefore(dueBy));
            ClaimStatus then = claim.statusAt(end);
            if (!inside || (then != ClaimStatus.OPEN && then != ClaimStatus.PENDING) || !person.countsAtEnd(claim, end, today)) {
                continue;
            }
            open = open.add(due(sheetId, person, claim, policyDate, today));
        }
        return open;
    }

    /** The auto close, once the sheet's today is set: with grace days set, every open period whose to-date plus
     *  them is before the sheet's today is closed, in order of their to-dates; a soft-closed period is left as it is. */
    public void closeDue(String sheetId, String user) {
        Integer grace = settings.graceDays(sheetId);
        if (grace == null) {
            return;
        }
        LocalDate today = sheets.today(sheetId);
        List<PeriodResponse> due = periods.list(sheetId).stream()
                .filter(p -> !p.closed() && !p.softClosed() && LocalDate.parse(p.to()).plusDays(grace).isBefore(today))
                .sorted(java.util.Comparator.comparing(PeriodResponse::to))
                .toList();
        for (PeriodResponse period : due) {
            PeriodResponse locked = periods.lock(sheetId, period.name());
            if (locked != null && !locked.closed() && !locked.softClosed()) {
                close(sheetId, locked, user, true);
            }
        }
    }
}
