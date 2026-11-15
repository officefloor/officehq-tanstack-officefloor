package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
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
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Rolls fiscal year FY<yyyy> over: makes the next fiscal year's twelve monthly periods exactly as the fiscal
 *  year generator does, then adds each person's open figure (a person with none: nothing) at the end of the old year's last period (the
 *  due, as shown now, of their claims that count, due by a day in a period of the old year, whose status at
 *  that period's end was OPEN or PENDING) to their opening balance for the new year's first period, each
 *  recorded as OPENING_SET, then ROLLOVER. Refused, with nothing written, for anyone but a steward, a year
 *  that is not four digits, or a year with no periods. */
public class RollOver {
    public void rollOver(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @RequestBody RolloverRequest request, SheetRepository sheets, SettingRepository settings,
            PeriodRepository periods, ClaimRepository claims, ClaimLogRepository log, PersonRepository people,
            RateRepository rates, RoleRepository roles, Audit audit, Out<GeneratePeriodsResult> result)
            throws InvalidPeriodException, PeriodPermissionException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (!Role.STEWARD.equals(roles.roleOf(id, user))) {
            throw new PeriodPermissionException("Only a steward can roll a year over");
        }
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("A fiscal year is four digits (yyyy)");
        }
        YearMonth start = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(id));
        LocalDate first = start.atDay(1);
        LocalDate next = start.plusMonths(12).atDay(1);
        List<Period> old = periods.list(id).stream().filter((p) -> {
            LocalDate from = LocalDate.parse(p.from());
            return !from.isBefore(first) && from.isBefore(next);
        }).toList();
        if (old.isEmpty()) {
            throw new InvalidPeriodException("FY" + year + " has no periods");
        }
        Map<Long, BigDecimal> open = openFigures(id, old, claims, log, people, rates, settings, sheets);

        sheets.open(id, user);
        int made = GeneratePeriods.months(id, user, start.plusMonths(12), start.plusMonths(23), periods, audit);
        Period firstNew = periods.containing(id, next.toString());
        if (firstNew != null) {
            for (Person p : people.list(id)) {
                BigDecimal amount = open.get(p.getId());
                // A person with no open figure has nothing to carry.
                if (amount == null) continue;
                periods.addOpening(id, firstNew.name(), p.getId(), amount);
                audit.record("OPENING_SET sheet=" + id + " by=" + user + " name=" + p.getName() + " period="
                        + firstNew.name() + " amount=" + amount.toPlainString());
            }
        }
        audit.record("ROLLOVER sheet=" + id + " by=" + user + " fy=FY" + year + " periods=" + made);
        result.set(new GeneratePeriodsResult(made));
    }

    /** Each person's open figure at the end of the last of the periods, by person id. */
    private static Map<Long, BigDecimal> openFigures(String id, List<Period> old, ClaimRepository claims,
            ClaimLogRepository log, PersonRepository people, RateRepository rates, SettingRepository settings,
            SheetRepository sheets) {
        DatedSetting floor = settings.floor(id);
        Dues.Late late = settings.late(id, sheets.today(id));
        String end = old.stream().map(Period::to).max(String::compareTo).orElseThrow();
        Map<Long, String> atEnd = log.statusesOn(id, LocalDate.parse(end));
        Map<Long, BigDecimal> open = new LinkedHashMap<>();
        for (Claim c : claims.list(id)) {
            if (old.stream().noneMatch((p) -> p.contains(c.getDueBy()))) continue;
            String status = atEnd.containsKey(c.getId()) ? atEnd.get(c.getId()) : c.getStatus();
            if (status == null || !ClaimStatus.isOpen(status)) continue;
            Person person = people.find(id, c.getPersonId());
            if (person == null || person.leftOutLive(c)) continue;
            Rate rate = rates.find(id, person.gradeOn(c.getDate()), LocalDate.parse(c.getDate()), c.getId());
            String due = Dues.shown(c, rate == null ? null : Dues.of(c.getAmount(), rate.pct(), late.rounding(c.getRoundDate())), floor.on(c.getDate()), late);
            if (due == null) continue;
            open.merge(person.getId(), new BigDecimal(due), BigDecimal::add);
        }
        return open;
    }
}
