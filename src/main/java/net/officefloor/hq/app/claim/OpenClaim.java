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

/** Opens a claim, giving it its number (see {@link ClaimNumbers}) and recording who opened it; one
 *  whose due is above the review threshold in force on its date goes to REVIEW instead, unnumbered
 *  until a steward reviews it (see {@link ReviewClaim}). Refused for a REVIEW claim,  for a role the permission matrix does not let open, and for a claim with an empty note
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
        ClaimStatus opened = opensTo(claim, people, rates, settings, sheets);
        requireNote(claim, people, rates, settings, sheets);
        requireUnderCap(claim, claims, periods);
        claim.setStatus(opened.name());
        if (opened == ClaimStatus.OPEN) {
            claim.setNumber(ClaimNumbers.next(claim, claims, periods, settings, sheets));
        }
        claim.setOpenedBy(by == null ? "" : by);
    }

    /** The status opening the claim moves it to: REVIEW for a DRAFT whose due (null: n/a, never above)
     *  is above the review threshold in force on its date, else OPEN; refused when its status allows
     *  neither, and for a REVIEW claim (only a steward's review opens it); shared with
     *  {@link BulkOpenClaims}. */
    static ClaimStatus opensTo(Claim claim, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) throws ClaimStatusException {
        ClaimStatus status = ClaimStatus.valueOf(claim.getStatus());
        if (status == ClaimStatus.REVIEW) {
            throw new ClaimStatusException("A REVIEW claim is opened by a steward's review");
        }
        ClaimStatus opened = status.moveTo(ClaimStatus.OPEN);
        if (status != ClaimStatus.DRAFT) return opened;
        BigDecimal threshold = settings.reviewThreshold(claim.getSheetId(), claim.getDate());
        if (threshold == null) return opened;
        String due = due(claim, people, rates, settings, sheets);
        return due != null && new BigDecimal(due).compareTo(threshold) > 0 ? ClaimStatus.REVIEW : opened;
    }

    /** The due the claim shows (null: n/a): bound, settled, else live at the rate in force on its date
     *  with its late charge on the sheet's today, at least the floor in force on its date. */
    static String due(Claim claim, PersonRepository people, RateRepository rates, SettingRepository settings,
            SheetRepository sheets) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        Rate rate = person == null ? null : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()),
                LocalDate.parse(claim.getDate()), claim.getId());
        Dues.Late late = settings.late(claim.getSheetId(), sheets.today(claim.getSheetId()));
        return Dues.shown(claim, rate == null ? null : Dues.of(claim.getAmount(), rate.pct(),
                late.rounding(claim.getRoundDate())), settings.floor(claim.getSheetId()).on(claim.getDate()), late);
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
        String due = due(claim, people, rates, settings, sheets);
        if (due != null && new BigDecimal(due).compareTo(threshold) > 0) {
            throw new ClaimStatusException("A claim whose due is above the note threshold needs a note to be opened");
        }
    }
}
