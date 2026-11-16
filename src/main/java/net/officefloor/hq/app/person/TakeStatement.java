package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.Dues;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.DatedSetting;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Takes a statement of the person: records their due-total now (the sum of the due of their claims that
 *  count live (open ones dated within a hold of theirs, and not bound, do not), n/a ones skipped, at most the sheet's cap less their cut total (not below 0.00); n/a when their grade has no rate on the sheet's today and none of
 *  those claims keeps a settled or bound figure, or a live one at a grade no longer in force or dated on or before the sheet's today) on the sheet's today, and binds each counting claim
 *  to the due it shows now, late charge and floor included (a claim already bound keeps its figure); a claim that does not
 *  count live is unbound.
 *  <p>Taken as of a date ({@code asof}; blank: now) on or before the sheet's today, it goes by everything as it
 *  stood at the end of that date (every change made while the sheet's today was on or before it counts, every
 *  later one does not), that date standing in for the sheet's today: it records that day's due-total under that
 *  date and binds the claims that counted then to their figures then. Refused for a date after the sheet's
 *  today, or before the person was on the sheet. */
public class TakeStatement {
    public void take(@RequestParam(name = "asof", required = false) String asof, @Val Person person,
            PersonRepository people, ClaimRepository claims, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, PoolRepository pools) throws InvalidPersonException {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        LocalDate on = today;
        Person then = person;
        List<Claim> own;
        List<Rate> sheetRates;
        DatedSetting floor;
        Dues.Late late;
        BigDecimal cap;
        if (asof == null || asof.isBlank()) {
            own = claims.listOf(sheetId, person.getId());
            sheetRates = rates.list(sheetId);
            floor = settings.floor(sheetId);
            late = settings.late(sheetId, today);
            cap = settings.cap(sheetId);
            if (cap != null) cap = Rollup.capLessCuts(cap, pools.cutTotals(sheetId).get(person.getId()));
        } else {
            try {
                on = LocalDate.parse(asof.trim());
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The statement date must be a date");
            }
            if (on.isAfter(today)) {
                throw new InvalidPersonException("The statement date cannot be after the sheet's today");
            }
            if (people.addedAfter(sheetId, person.getId(), on)) {
                throw new InvalidPersonException("The person was not on the sheet on the statement date");
            }
            then = people.findAsOf(sheetId, person.getId(), on);
            own = claims.listOfAsOf(sheetId, person.getId(), on);
            sheetRates = rates.listAsOf(sheetId, on);
            floor = settings.datedAsOf(sheetId, SettingRepository.FLOOR, SettingRepository.DEFAULT_FLOOR, on);
            late = settings.lateAsOf(sheetId, on);
            cap = settings.capAsOf(sheetId, on);
            if (cap != null) cap = Rollup.capLessCuts(cap, pools.cutTotalsAsOf(sheetId, on).get(person.getId()));
            person.setStatedAsOf(on.toString());
        }
        // The figure each counting claim is bound to, by claim id.
        Map<Long, String> figures = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean kept = false;
        for (Claim c : own) {
            if (!then.countsLive(c)) continue;
            String grade = then.gradeOn(c.getDate());
            Rate rate = RateRepository.inForce(sheetRates, grade, c.getDate(), c.getId());
            String live = rate == null ? null : Dues.of(c.getAmount(), rate.pct(), late.rounding(c.getRoundDate()));
            String shown = Dues.shown(c, live, floor.on(c.getDate()), late);
            kept |= c.getSettledDue() != null || c.isBound()
                    || (rate != null && (!grade.equals(then.getGrade())
                            || !LocalDate.parse(c.getDate()).isAfter(on)));
            if (shown != null) total = total.add(new BigDecimal(shown));
            // The due as shown is bound, late charge included: it no longer follows the sheet's today.
            figures.put(c.getId(), shown);
        }
        for (Claim c : claims.listOf(sheetId, person.getId())) {
            if (figures.containsKey(c.getId())) c.bind(true, figures.get(c.getId()));
            else c.bind(false, null);
            claims.updateBinding(c);
        }
        boolean na = RateRepository.inForce(sheetRates, then.getGrade(), on.toString()) == null && !kept;
        if (cap != null && total.compareTo(cap) > 0) total = cap.setScale(2);
        person.stated(na ? null : total.toPlainString(), on.toString(), figures.size());
        people.updateStatements(person);
    }
}
