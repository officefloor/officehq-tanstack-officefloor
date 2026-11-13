package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * (its date plus the terms of its person's grade in force on that date), and the original's number as split-from.
 * The new claim is published as the claim, for the steps that follow.
 */
public class SplitClaim {
    public void split(@RequestParam("user") String user, @Val Claim claim, @Val SplitAmount amount,
            SheetRepository sheets, PersonRepository people, SettingRepository settings, PeriodRepository periods,
            ClaimRepository claims, Out<ClaimSplit> split, Out<Claim> created) throws InvalidClaimException {
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
        LocalDate dueBy = claim.date().plusDays(person == null
                ? settings.dated(claim.sheetId()).termsFor(null, claim.date())
                : person.termsOn(settings.dated(claim.sheetId()), claim.date()));
        String number = ApplyClaimAction.nextNumber(claim, sheets, settings, periods);
        Claim saved = claims.save(new Claim(null, claim.sheetId(), claim.personId(), off, claim.date(), claim.note(),
                ClaimStatus.OPEN, number, null, user, false, null, null, dueBy, null, 0, claim.number()));
        split.set(new ClaimSplit(original, saved));
        created.set(saved);
    }
}
