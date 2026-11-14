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
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Closes a period, fixing its due-total as it is now: the due, as the screen shows it now, of the claims
 *  whose date falls in it that counted by their status at the end of its last day as their status log
 *  records it (n/a ones skipped, and those the live rules leave out: deferred, or OPEN ones dated within
 *  a hold of their person, unless bound).
 *  Each person's due-total for it is fixed too, for everyone on the sheet. Refused when it is already closed, and for a role
 *  the permission matrix does not let close. */
public class ClosePeriod {
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestParam(name = "by", required = false) String by, PeriodRepository periods,
            ClaimRepository claims, ClaimLogRepository log, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets, RoleRepository roles,
            Out<Period> period) throws InvalidPeriodException, PeriodClosedException, PeriodPermissionException {
        String id = sheetId.trim();
        Period found = periods.find(id, name);
        if (found == null) {
            throw new InvalidPeriodException("No such period: " + name);
        }
        if (!Permissions.allows(id, by, Permissions.CLOSE, roles, settings)) {
            throw new PeriodPermissionException("Your role may not close a period");
        }
        if (found.closed()) {
            throw new PeriodClosedException("The period is already closed");
        }
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
            String due = Dues.shown(c, rate == null ? null : Dues.of(c.getAmount(), rate.pct(), floor.on(c.getDate()), late.rounding(c.getRoundDate())), late);
            if (due != null) {
                total = total.add(new BigDecimal(due));
                if (person != null) personTotals.merge(person.getId(), new BigDecimal(due), BigDecimal::add);
            }
        }
        period.set(periods.close(found, total, personTotals, by == null ? "" : by));
    }
}
