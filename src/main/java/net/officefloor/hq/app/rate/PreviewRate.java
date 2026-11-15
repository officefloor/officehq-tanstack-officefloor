package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Dues;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.DatedSetting;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Preview a candidate rate for a grade on every date: the open (OPEN or PENDING) claims of people on
 *  that grade on the claim's date whose shown due would differ, and the sum of the differences. A claim
 *  bound by a statement keeps its figure; a settled one is not open. Reads only: nothing is written. */
public class PreviewRate {
    public void preview(@PathVariable("sheetId") String sheetId, @Val ValidRate candidate,
            ClaimRepository claims, PersonRepository people, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, ObjectResponse<RatePreview> response) {
        String id = sheetId.trim();
        List<Rate> sheetRates = rates.list(id);
        Map<Long, Person> byId = people.list(id).stream().collect(Collectors.toMap(Person::getId, Function.identity()));
        DatedSetting floor = settings.floor(id);
        Dues.Late late = settings.late(id, sheets.today(id));
        String pct = candidate.pct().toPlainString();
        int count = 0;
        BigDecimal diff = BigDecimal.ZERO.setScale(2);
        for (Claim c : claims.list(id)) {
            if (!ClaimStatus.isOpen(c.getStatus())) continue;
            Person p = byId.get(c.getPersonId());
            if (p == null || !candidate.grade().equals(p.gradeOn(c.getDate()))) continue;
            Rate rate = RateRepository.inForce(sheetRates, candidate.grade(), c.getDate());
            String now = Dues.shown(c, rate == null ? null
                    : Dues.of(c.getAmount(), rate.pct(), late.rounding(c.getRoundDate())), floor.on(c.getDate()), late);
            String then = Dues.shown(c, Dues.of(c.getAmount(), pct, late.rounding(c.getRoundDate())),
                    floor.on(c.getDate()), late);
            BigDecimal before = now == null ? BigDecimal.ZERO.setScale(2) : new BigDecimal(now);
            BigDecimal after = then == null ? BigDecimal.ZERO.setScale(2) : new BigDecimal(then);
            if (now == null ? then == null : then != null && before.compareTo(after) == 0) continue;
            count++;
            diff = diff.add(after.subtract(before));
        }
        response.send(new RatePreview(count, diff.setScale(2).toPlainString()));
    }
}
