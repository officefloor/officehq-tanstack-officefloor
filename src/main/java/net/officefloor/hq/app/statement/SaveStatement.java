package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Grades;
import net.officefloor.hq.app.person.Holds;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonResponse;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.DatedAmount;
import net.officefloor.hq.app.setting.SettingHistory;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Take the person's statement: record their due-total now (late charges included, at most the sheet's cap) and the sheet's today, bind
 *  each of their claims that counts to its rate figure now (never the floor or its late charge, which keep following
 *  the sheet's floor and today), and unbind the rest (their due is live again; a claim deferred on today is left unbound). A settled claim's
 *  fixed due is left as it is.
 *  <p>Taken as of a date (on or before the sheet's today; refused after it, or for a person added after it), the
 *  same is done with the sheet as it stood at the end of that date: every change made while the sheet's today was on
 *  or before it counts, every later one does not, and the date stands in for the sheet's today. Their claims that
 *  counted then are bound to their figures then, the rest are unbound, and the date is recorded as the statement's. */
public class SaveStatement {
    public void save(@Val StatementRequest request, @Val Person loaded, PersonRepository people,
            ClaimRepository claims, RateRepository rates, SheetRepository sheets, SettingRepository settings,
            Out<Statement> statement, Out<PersonResponse> saved) throws InvalidPersonException {
        people.lock(loaded);
        Person person = people.find(loaded.sheetId(), loaded.id());
        String sheetId = person.sheetId();
        LocalDate today = sheets.today(sheetId);
        LocalDate asOf = request.asOf() != null ? LocalDate.parse(request.asOf()) : null;
        if (asOf != null && today != null && asOf.isAfter(today)) {
            throw new InvalidPersonException("A statement cannot be taken as of a date after the sheet's today ("
                    + today + ").");
        }
        if (asOf != null && !people.addedBy(person, asOf)) {
            throw new InvalidPersonException(person.name() + " was not on the sheet on " + asOf + ".");
        }
        LocalDate on = asOf != null ? asOf : today;
        List<Claim> theirs = claims.listForPerson(sheetId, person.id());
        Rounding now = Rounding.of(sheetId, settings, sheets, claims);

        List<Claim> then;
        Grades grades;
        Holds holds;
        LocalDate retiredFrom;
        Rates ratesThen;
        DatedAmount floor;
        LateCharge late;
        BigDecimal cap;
        if (asOf == null) {
            then = theirs;
            grades = person.grades();
            holds = person.holds();
            retiredFrom = person.retiredFrom();
            ratesThen = rates;
            floor = settings.floor(sheetId);
            late = LateCharge.of(sheetId, settings, sheets, claims, people);
            cap = settings.cap(sheetId);
        } else {
            then = claims.listForPersonAsOf(sheetId, person.id(), asOf);
            grades = people.gradesAsOf(person, asOf);
            holds = people.holdsAsOf(person, asOf);
            retiredFrom = people.retiredFromAsOf(person, asOf);
            ratesThen = rates.asOf(sheetId, asOf);
            SettingHistory history = settings.historyAsOf(sheetId, asOf);
            floor = history::floorOn;
            LocalDate policy = settings.policyDate(sheetId);
            late = new LateCharge(history::latePctOn, asOf, new Rounding(policy != null ? policy : asOf,
                    now.numbered(), history.of(SettingHistory.ROUNDING)), LateCharge.Grace.of(history, Map.of(person.id(), grades)));
            cap = history.capOn(asOf);
        }

        BigDecimal total = Dues.capped(Dues.total(sheetId, then, grades, holds, retiredFrom, on, ratesThen, floor,
                late), cap);
        Map<Long, BigDecimal> bound = new HashMap<>();
        for (Claim c : then) {
            if (Dues.counts(c, holds, retiredFrom, on)) {
                bound.put(c.id(), Dues.figure(c, grades, ratesThen, late.rounding()));
            }
        }
        for (Claim c : theirs) {
            if (bound.containsKey(c.id())) {
                claims.state(c, true, bound.get(c.id()));
            } else if (c.stated()) {
                claims.state(c, false, null);
            }
        }
        people.stated(person, total, on);
        statement.set(new Statement(sheetId, person.name(), total, bound.size(), asOf));
        saved.set(PersonResponse.from(people.find(sheetId, person.id())));
    }
}
