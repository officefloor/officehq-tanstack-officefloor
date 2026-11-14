package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.Dues;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Closes a period, fixing its due-total as it is now: the due of the claims that count whose date
 *  falls in it (n/a ones skipped, and OPEN ones dated within a hold of their person, unless bound), as the screen shows them.
 *  Refused when it is already closed. */
public class ClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestParam(name = "by", required = false) String by, PeriodRepository periods,
            ClaimRepository claims, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets,
            Out<Period> period) throws InvalidPeriodException, PeriodClosedException {
        String id = sheetId.trim();
        Period found = periods.find(id, name);
        if (found == null) {
            throw new InvalidPeriodException("No such period: " + name);
        }
        if (found.closed()) {
            throw new PeriodClosedException("The period is already closed");
        }
        String floor = settings.floor(id);
        Dues.Late late = settings.late(id, sheets.today(id));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim c : claims.list(id)) {
            if (!c.isCounting() || !found.contains(c.getDate())) continue;
            Person person = people.find(id, c.getPersonId());
            if (person != null && !person.countsLive(c)) continue;
            Rate rate = person == null ? null
                    : rates.find(id, person.gradeOn(c.getDate()), LocalDate.parse(c.getDate()));
            String due = Dues.shown(c, rate == null ? null : Dues.of(c.getAmount(), rate.pct(), floor, late.rounding(c.getRoundDate())), late);
            if (due != null) total = total.add(new BigDecimal(due));
        }
        period.set(periods.close(found, total, by == null ? "" : by));
    }
}
