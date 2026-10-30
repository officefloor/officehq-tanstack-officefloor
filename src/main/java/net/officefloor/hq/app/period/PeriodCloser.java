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
import org.springframework.stereotype.Service;

/** Closes a period, fixing its due-total as it is now: the dues as shown now of the claims dated in it that counted
 *  at the end of its last day (see {@link Person#countsAtEnd}), each with its late charge, and each person's share of it (a person's unbound OPEN claims dated within one of their holds count in none; a claim whose due is n/a adds nothing).
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

    public PeriodCloser(Periods periods, People people, Claims claims, Rates rates, Settings settings, Sheets sheets,
            Audit audit) {
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
                        : Rates.due(claim.getAmount(), rates.find(sheetId, person.gradeOn(claim.getDate()), claim.getDate()), rounding);
                if (figure != null) {
                    personTotal = personTotal.add(LateCharge.due(claim, figure, settings.floor(sheetId, claim.getDate()),
                            on -> settings.latePct(sheetId, on), today, rounding));
                }
            }
            personTotals.put(person.getId(), personTotal);
            total = total.add(personTotal);
        }
        periods.close(sheetId, period.name(), total, personTotals, soft);
        audit.recordOnCommit((soft ? "PERIOD_SOFT_CLOSED" : "PERIOD_CLOSED") + " sheet=" + sheetId + " by=" + user
                + " period=" + period.name() + " total=" + total.toPlainString() + (auto ? " auto=yes" : ""));
    }

    /** The auto close, once the sheet's today is set: with grace days set, every open period whose to-date plus
     *  them is before the sheet's today is closed, in order of their to-dates. */
    public void closeDue(String sheetId, String user) {
        Integer grace = settings.graceDays(sheetId);
        if (grace == null) {
            return;
        }
        LocalDate today = sheets.today(sheetId);
        List<PeriodResponse> due = periods.list(sheetId).stream()
                .filter(p -> !p.closed() && LocalDate.parse(p.to()).plusDays(grace).isBefore(today))
                .sorted(java.util.Comparator.comparing(PeriodResponse::to))
                .toList();
        for (PeriodResponse period : due) {
            PeriodResponse locked = periods.lock(sheetId, period.name());
            if (locked != null && !locked.closed()) {
                close(sheetId, locked, user, true);
            }
        }
    }
}
