package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Dues;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.DatedSetting;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** How a period is closed: shared by a close by hand ({@link ClosePeriod}) and the auto close of a
 *  today change ({@link AutoClosePeriods}). */
public final class PeriodClosing {
    private PeriodClosing() {
    }

    /** Close the open period now, as a close by hand does (no permission check): used by the
     *  sheet's today change to close the periods past their grace days. */
    public static Period closeNow(Period found, String user, PeriodRepository periods, ClaimRepository claims,
            ClaimLogRepository log, PersonRepository people, RateRepository rates, SettingRepository settings,
            SheetRepository sheets) {
        String id = found.sheetId();
        DatedSetting floor = settings.floor(id);
        Dues.Late late = settings.late(id, sheets.today(id));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        for (Person p : people.list(id)) personTotals.put(p.getId(), BigDecimal.ZERO.setScale(2));
        Map<Long, String> atEnd = log.statusesOn(id, LocalDate.parse(found.to()));
        for (Claim c : claims.list(id)) {
            if (!found.contains(c.getDate())) continue;
            String status = atEnd.containsKey(c.getId()) ? atEnd.get(c.getId()) : c.getStatus();
            if (status == null || !(ClaimStatus.isOpen(status) || ClaimStatus.SETTLED.name().equals(status))) continue;
            Person person = people.find(id, c.getPersonId());
            if (person != null && person.leftOutLive(c)) continue;
            Rate rate = person == null ? null
                    : rates.find(id, person.gradeOn(c.getDate()), LocalDate.parse(c.getDate()));
            String due = Dues.shown(c, rate == null ? null : Dues.of(c.getAmount(), rate.pct(), late.rounding(c.getRoundDate())), floor.on(c.getDate()), late);
            if (due != null) {
                total = total.add(new BigDecimal(due));
                if (person != null) personTotals.merge(person.getId(), new BigDecimal(due), BigDecimal::add);
            }
        }
        return periods.close(found, total, personTotals, user);
    }
}
