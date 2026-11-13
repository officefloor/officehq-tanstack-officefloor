package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogLine;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.DatedSettings;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Component;

/**
 * Closes a period, fixing its due-total as it stands now: the dues (figures plus any late charge, as they are now) of
 * the claims dated in it that count by their status at the period's end, added up (n/a skipped), and each person's
 * share of it (0.00 for a person with none). Used by a close by hand and by the automatic close when today changes.
 */
@Component
public class PeriodCloser {

    private final ClaimRepository claims;
    private final ClaimLogRepository logs;
    private final PersonRepository people;
    private final RateRepository rates;
    private final SettingRepository settings;
    private final SheetRepository sheets;

    public PeriodCloser(ClaimRepository claims, ClaimLogRepository logs, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) {
        this.claims = claims;
        this.logs = logs;
        this.people = people;
        this.rates = rates;
        this.settings = settings;
        this.sheets = sheets;
    }

    /** The same period, closed with its due-total and each person's due-total fixed as they stand now. */
    public Period close(Period period) {
        return fix(period, false);
    }

    /** The same period, soft-closed with its due-total and each person's due-total fixed as they stand now. */
    public Period softClose(Period period) {
        return fix(period, true);
    }

    private Period fix(Period period, boolean soft) {
        Map<Long, Person> byId = people.findAll(period.sheetId()).stream()
                .collect(Collectors.toMap(Person::id, Function.identity()));
        List<Rate> sheetRates = rates.findAll(period.sheetId());
        DatedSettings dated = settings.dated(period.sheetId());
        LocalDate today = sheets.today(period.sheetId());
        List<Claim> sheetClaims = claims.findAll(period.sheetId());
        Map<Long, List<ClaimLogLine>> sheetLogs = logs.findAll(period.sheetId());
        Rounding rounding = settings.rounding(period.sheetId(), today, sheetClaims);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> perPerson = new HashMap<>();
        for (Long id : byId.keySet()) {
            perPerson.put(id, BigDecimal.ZERO.setScale(2));
        }
        for (Claim claim : sheetClaims) {
            Person person = byId.get(claim.personId());
            if (person == null || !period.counts(claim, sheetLogs.getOrDefault(claim.id(), List.of()), person, today)) {
                continue;
            }
            BigDecimal figure = claim.due(Rate.inForce(sheetRates, person.gradeOn(claim.date()), claim.date()),
                    dated.floorOn(claim.date()), dated.lateCharge(today, person), rounding);
            if (figure != null) {
                total = total.add(figure);
                perPerson.merge(person.id(), figure, BigDecimal::add);
            }
        }
        return soft ? period.softClose(total, perPerson) : period.close(total, perPerson);
    }
}
