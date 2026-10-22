package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Grades;
import net.officefloor.hq.app.person.Holds;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.Dues;
import net.officefloor.hq.app.statement.LateCharge;

/** Closing a period as a close by hand does, shared by the close and the today change's auto close. */
public final class PeriodCloser {

    private PeriodCloser() {
    }

    /** Close the open or soft-closed period, fixing its due-total and each person's afresh as they are now (as a close
     *  by hand does): the period as closed. Refused when it is already closed. */
    public static Period fix(String id, Period period, PeriodRepository periods, ClaimRepository claims, ClaimLogRepository logs,
            PersonRepository people, RateRepository rates, SettingRepository settings, SheetRepository sheets)
            throws InvalidPeriodException {
        return fix(id, period, false, periods, claims, logs, people, rates, settings, sheets);
    }

    /** Close (or, soft, soft-close) the period, fixing its due-total and each person's as they are now: the period
     *  as closed or soft-closed. Refused when it is already closed (or, soft, already closed or soft-closed). */
    public static Period fix(String id, Period period, boolean soft, PeriodRepository periods, ClaimRepository claims,
            ClaimLogRepository logs, PersonRepository people, RateRepository rates, SettingRepository settings,
            SheetRepository sheets) throws InvalidPeriodException {
        Map<Long, Person> byId = people.list(id).stream().collect(Collectors.toMap(Person::id, Function.identity()));
        var floor = settings.floor(id);
        LateCharge late = LateCharge.of(id, settings, sheets, claims);
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        for (Long personId : byId.keySet()) {
            personTotals.put(personId, new BigDecimal("0.00"));
        }
        Map<Long, ClaimStatus> atEnd = logs.statusesAt(id, period.to());
        BigDecimal total = new BigDecimal("0.00");
        for (Claim c : claims.list(id)) {
            Person person = byId.get(c.personId());
            ClaimStatus status = atEnd.containsKey(c.id()) ? atEnd.get(c.id()) : c.status();
            if (!Dues.countsAtEnd(c, status, person != null ? person.holds() : Holds.NONE,
                    person != null ? person.retiredFrom() : null, late.today())
                    || !period.contains(c.date())) {
                continue;
            }
            BigDecimal figure = Dues.shown(c, person != null ? person.grades() : Grades.NONE, rates, floor, late);
            if (figure != null) {
                total = total.add(figure);
                if (person != null) {
                    personTotals.merge(person.id(), figure, BigDecimal::add);
                }
            }
        }
        if (soft ? !periods.softClose(period, total, personTotals) : !periods.close(period, total, personTotals)) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        return new Period(period.sheetId(), period.name(), period.from(), period.to(), !soft, total, soft);
    }
}
