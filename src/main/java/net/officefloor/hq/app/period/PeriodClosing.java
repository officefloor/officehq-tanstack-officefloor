package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
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
        return fixNow(found, user, false, periods, claims, log, people, rates, settings, sheets);
    }

    /** Close or (soft) soft-close the period now, fixing its figures (and everyone's carried figure) as they are now (no permission check). */
    public static Period fixNow(Period found, String user, boolean soft, PeriodRepository periods, ClaimRepository claims,
            ClaimLogRepository log, PersonRepository people, RateRepository rates, SettingRepository settings,
            SheetRepository sheets) {
        String id = found.sheetId();
        DatedSetting floor = settings.floor(id);
        Dues.Late late = settings.late(id, sheets.today(id));
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Map<Long, BigDecimal> personTotals = new LinkedHashMap<>();
        Map<Long, BigDecimal> personCarried = new LinkedHashMap<>();
        for (Person p : people.list(id)) {
            personTotals.put(p.getId(), BigDecimal.ZERO.setScale(2));
            personCarried.put(p.getId(), BigDecimal.ZERO.setScale(2));
        }
        List<Period> earlier = periods.list(id).stream().filter((e) -> e.to().compareTo(found.from()) < 0).toList();
        Map<Long, String> atEnd = log.statusesOn(id, LocalDate.parse(found.to()));
        for (Claim c : claims.list(id)) {
            boolean in = found.contains(c.getDueBy());
            // Carried: due by a day in an earlier period, still OPEN or PENDING at the end of this one.
            boolean carried = !in && earlier.stream().anyMatch((e) -> e.contains(c.getDueBy()));
            if (!in && !carried) continue;
            String status = atEnd.containsKey(c.getId()) ? atEnd.get(c.getId()) : c.getStatus();
            if (status == null || !(ClaimStatus.isOpen(status) || (in && ClaimStatus.SETTLED.name().equals(status)))) continue;
            Person person = people.find(id, c.getPersonId());
            if (person != null && person.leftOutLive(c)) continue;
            Rate rate = person == null ? null
                    : rates.find(id, person.gradeOn(c.getDate()), LocalDate.parse(c.getDate()), c.getId());
            String due = Dues.shown(c, rate == null ? null : Dues.of(c.getAmount(), rate.pct(), late.rounding(c.getRoundDate())), floor.on(c.getDate()), late);
            if (due == null) continue;
            if (carried) {
                if (person != null) personCarried.merge(person.getId(), new BigDecimal(due), BigDecimal::add);
                continue;
            }
            total = total.add(new BigDecimal(due));
            if (person != null) personTotals.merge(person.getId(), new BigDecimal(due), BigDecimal::add);
        }
        // An opening balance is part of the carried figure fixed now.
        found.personOpenings().forEach((pid, amount) -> {
            if (personCarried.containsKey(pid)) personCarried.merge(pid, new BigDecimal(amount), BigDecimal::add);
        });
        return periods.close(found, total, personTotals, personCarried, user, soft);
    }
}
