package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.DatedSettings;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Closes the period, fixing its due-total as it stands now: the dues (figures plus any late charge) of the counting
 * claims dated in it, added up (n/a skipped), and each person's share of it (0.00 for a person with none). Refused (400) when it is already closed.
 */
public class ClosePeriod {
    public void close(@Val Period period, ClaimRepository claims, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets, Out<Period> closed) throws InvalidPeriodException {
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        Map<Long, Person> byId = people.findAll(period.sheetId()).stream()
                .collect(Collectors.toMap(Person::id, Function.identity()));
        List<Rate> sheetRates = rates.findAll(period.sheetId());
        DatedSettings dated = settings.dated(period.sheetId());
        LocalDate today = sheets.today(period.sheetId());
        List<Claim> sheetClaims = claims.findAll(period.sheetId());
        Rounding rounding = settings.rounding(period.sheetId(), today, sheetClaims);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> perPerson = new HashMap<>();
        for (Long id : byId.keySet()) {
            perPerson.put(id, BigDecimal.ZERO.setScale(2));
        }
        for (Claim claim : sheetClaims) {
            Person person = byId.get(claim.personId());
            if (person == null || !person.counts(claim, today) || !period.contains(claim.date())) {
                continue;
            }
            BigDecimal figure = claim.due(Rate.inForce(sheetRates, person.gradeOn(claim.date()), claim.date()),
                    dated.floorOn(claim.date()), dated.lateCharge(claim.date(), today), rounding);
            if (figure != null) {
                total = total.add(figure);
                perPerson.merge(person.id(), figure, BigDecimal::add);
            }
        }
        closed.set(period.close(total, perPerson));
    }
}
