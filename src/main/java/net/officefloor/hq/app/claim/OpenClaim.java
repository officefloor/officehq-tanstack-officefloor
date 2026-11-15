package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Opens a claim, giving it its number (see {@link ClaimNumbers}) and recording who opened it.
 *  Refused for a role the permission matrix does not let open, and for a claim with an empty note
 *  whose due is above the note threshold in force on its date. */
public class OpenClaim {
    public void move(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            ClaimRepository claims, PeriodRepository periods, SettingRepository settings,
            SheetRepository sheets, RoleRepository roles, PersonRepository people, RateRepository rates)
            throws ClaimStatusException, PeriodClosedException {
        if (!Permissions.allows(claim.getSheetId(), by, Permissions.OPEN, roles, settings)) {
            throw new ClaimStatusException("Your role may not open a claim");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        ClaimStatus opened = ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.OPEN);
        requireNote(claim, people, rates, settings, sheets);
        requireUnderCap(claim, claims, periods);
        claim.setStatus(opened.name());
        claim.setNumber(ClaimNumbers.next(claim, claims, periods, settings, sheets));
        claim.setOpenedBy(by == null ? "" : by);
    }

    /** Refuses opening a claim that belongs to a period with a cap (its due-by falls in it) when its
     *  person already has that many OPEN or PENDING claims in the period; shared with {@link BulkOpenClaims}. */
    static void requireUnderCap(Claim claim, ClaimRepository claims, PeriodRepository periods)
            throws ClaimStatusException {
        Period period = periods.containing(claim.getSheetId(), claim.getDueBy());
        if (period == null || period.cap() == null) return;
        long held = claims.list(claim.getSheetId()).stream()
                .filter((c) -> c.getId() != claim.getId() && c.getPersonId() == claim.getPersonId()
                        && ClaimStatus.isOpen(c.getStatus()) && period.contains(c.getDueBy()))
                .count();
        if (held >= period.cap()) {
            throw new ClaimStatusException("The person already has " + period.cap()
                    + " open claims in period " + period.name());
        }
    }

    /** Refuses opening a claim with an empty note whose due (null: n/a, never above) is above the
     *  sheet's note threshold in force on the claim's date; shared with {@link BulkOpenClaims}. */
    static void requireNote(Claim claim, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) throws ClaimStatusException {
        if (claim.getNote() != null && !claim.getNote().trim().isEmpty()) return;
        BigDecimal threshold = settings.noteThreshold(claim.getSheetId(), claim.getDate());
        if (threshold == null) return;
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Rate rate = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()),
                LocalDate.parse(claim.getDate()));
        Dues.Late late = settings.late(claim.getSheetId(), sheets.today(claim.getSheetId()));
        String due = Dues.shown(claim, rate == null ? null : Dues.of(claim.getAmount(), rate.pct(),
                late.rounding(claim.getRoundDate())), settings.floor(claim.getSheetId()).on(claim.getDate()), late);
        if (due != null && new BigDecimal(due).compareTo(threshold) > 0) {
            throw new ClaimStatusException("A claim whose due is above the note threshold needs a note to be opened");
        }
    }
}
