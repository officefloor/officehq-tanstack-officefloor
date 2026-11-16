package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimDue;
import net.officefloor.hq.app.claim.ClaimLogLine;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/** Closes an open period, fixing its due-total as it is now (see ClosePeriod): used by a close by hand and by the
 *  automatic close a change of the sheet's today makes. */
@Service
public class PeriodCloser {
    private final PeriodRepository periods;
    private final ClaimRepository claims;
    private final PersonRepository people;
    private final RateRepository rates;
    private final SettingRepository settings;
    private final SheetRepository sheets;
    private final ClaimLogRepository log;

    public PeriodCloser(PeriodRepository periods, ClaimRepository claims, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets, ClaimLogRepository log) {
        this.periods = periods;
        this.claims = claims;
        this.people = people;
        this.rates = rates;
        this.settings = settings;
        this.sheets = sheets;
        this.log = log;
    }

    /** Closes the period, returning the due-total it fixed; one already closed is refused. A soft-closed one is closed
     *  with its figures fixed afresh. */
    public BigDecimal close(Period period) throws InvalidPeriodException {
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed");
        }
        Map<Long, BigDecimal> perPerson = figures(period);
        BigDecimal total = perPerson.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (!periods.close(period, total)) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed");
        }
        periods.fixPersonTotals(period, perPerson);
        return total;
    }

    /** Soft-closes an open period, fixing its figures as a close does, returning the due-total it fixed; one closed or
     *  soft-closed already is refused. */
    public BigDecimal softClose(Period period) throws InvalidPeriodException {
        if (period.closed() || period.soft()) {
            throw new InvalidPeriodException("The period " + period.name() + " is not open");
        }
        Map<Long, BigDecimal> perPerson = figures(period);
        BigDecimal total = perPerson.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (!periods.softClose(period, total)) {
            throw new InvalidPeriodException("The period " + period.name() + " is not open");
        }
        periods.fixPersonTotals(period, perPerson);
        return total;
    }

    /** Each person's due-total for the period as it is now, by person id (see ClosePeriod). */
    private Map<Long, BigDecimal> figures(Period period) {
        Map<Long, Person> byId = people.list(period.sheetId()).stream()
                .collect(Collectors.toMap(Person::id, (p) -> p));
        Map<Long, List<ClaimLogLine>> logs = log.bySheet(period.sheetId());
        LateCharge late = LateCharge.of(period.sheetId(), sheets, settings, claims);
        Map<Long, BigDecimal> perPerson = new LinkedHashMap<>();
        for (Long id : byId.keySet()) {
            perPerson.put(id, BigDecimal.ZERO.setScale(2));
        }
        for (Claim c : claims.list(period.sheetId())) {
            Person person = byId.get(c.personId());
            if (c.dueBy().isBefore(period.from()) || c.dueBy().isAfter(period.to())) {
                continue;
            }
            ClaimStatus status = ClaimDue.statusAsOf(logs.getOrDefault(c.id(), List.of()), period.to());
            if (status == null || !ClaimDue.counts(ClaimDue.withStatus(c, status))
                    || (ClaimDue.isOpen(c) && !ClaimDue.bound(c) && person != null && person.heldOn(c.date()))
                    || ClaimDue.retiredOut(c, person, late.today())
                    || ClaimDue.deferred(c, late.today())) {
                continue;
            }
            BigDecimal figure = person == null ? c.due() : late.add(c, ClaimDue.figure(c, person.gradeOn(c.date()), rates, late.rounding()), settings.floor(c.sheetId(), c.date()));
            if (figure != null) {
                perPerson.merge(c.personId(), figure, BigDecimal::add);
            }
        }
        return perPerson;
    }
}
