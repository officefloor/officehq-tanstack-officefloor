package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Splits an amount off an OPEN claim that is not deferred; refused (400) for any other claim, for an amount not more
 * than 0.00 and less than the claim's, and to a role the sheet's permission matrix does not allow to open a claim.
 * The claim keeps the rest of its amount and loses any figure a statement bound it to. The new claim is OPEN, with
 * the split amount, the same date and note, the number opening gives it now, opened by the user, its due-by fixed now
 * (as a new claim's: the terms-th business day after its date, see {@link NewDueBy}), and the original's number as split-from, and the original's tags;
 * it is numbered within the period containing its own due-by, and is a late entry when the original is.
 * The new claim is published as the claim, for the steps that follow.
 */
public class SplitClaim {
    public void split(@RequestParam("user") String user, @Val Claim claim, @Val SplitAmount amount,
            SheetRepository sheets, PersonRepository people, SettingRepository settings, PeriodRepository periods,
            HolidayRepository holidays, ClaimRepository claims, Out<ClaimSplit> split, Out<Claim> created) throws InvalidClaimException {
        if (claim.status() != ClaimStatus.OPEN) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be split: only an OPEN one.");
        }
        if (claim.deferred(sheets.today(claim.sheetId()))) {
            throw new InvalidClaimException("The claim is deferred until " + claim.deferredUntil() + ": it cannot be split.");
        }
        if (!settings.find(claim.sheetId()).may(user, "open")) {
            throw new InvalidClaimException("The user's role is not allowed to open a claim, so cannot split one.");
        }
        BigDecimal off = amount.value();
        if (off.signum() <= 0 || off.compareTo(claim.amount()) >= 0) {
            throw new InvalidClaimException("The amount split off must be more than 0.00 and less than the claim's "
                    + claim.amount().setScale(2).toPlainString() + ".");
        }
        Claim original = claim.splitKeeping(claim.amount().subtract(off));
        claims.save(original);
        claims.bindStatement(original, false, null);
        Person person = people.find(claim.sheetId(), claim.personId());
        LocalDate dueBy = person == null
                ? holidays.businessDaysAfter(claim.sheetId(), claim.date(),
                        settings.dated(claim.sheetId()).termsFor(null, sheets.today(claim.sheetId())))
                : NewDueBy.of(claim.date(), person, sheets, settings, holidays);
        Claim made = new Claim(null, claim.sheetId(), claim.personId(), off, claim.date(), claim.note(),
                ClaimStatus.OPEN, null, null, user, false, null, null, dueBy, null, 0, claim.number());
        String number = ClaimNumbers.next(made, sheets, settings, periods);
        Claim saved = claims.save(made.withStatus(ClaimStatus.OPEN, number, null, user));
        claims.saveTags(saved, claims.tags(claim.sheetId(), claim.id()));
        if (claims.lateEntry(claim.sheetId(), claim.id())) {
            claims.markLateEntry(saved);
        }
        split.set(new ClaimSplit(original, saved));
        created.set(saved);
    }
}
