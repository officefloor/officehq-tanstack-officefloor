package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Splits an OPEN claim: it keeps the rest of its amount and is unbound; a new OPEN claim of the
 *  split-off amount, with its date and note, the next number, opened by the user and a due-by fixed
 *  now from the terms in force on its date, is split from its number (a late entry when its due-by
 *  falls in a soft-closed period). The split is judged by the original claim alone: refused for a claim
 *  due by a day in a closed period, one not OPEN or deferred, a role that may not open a claim, and an amount that is
 *  not more than 0.00 and less than the claim's (two decimals at most). */
public class SplitClaim {
    public void split(@RequestParam(name = "by", required = false) String by,
            @RequestParam(name = "amount", required = false) String amount, @Val Claim claim,
            ClaimRepository claims, PeriodRepository periods, SettingRepository settings,
            SheetRepository sheets, RoleRepository roles, PersonRepository people, Out<Split> split)
            throws ClaimStatusException, InvalidClaimException, PeriodClosedException {
        String sheetId = claim.getSheetId();
        periods.requireOpen(sheetId, claim.getDueBy());
        if (!ClaimStatus.OPEN.name().equals(claim.getStatus()) || claim.isDeferred()) {
            throw new ClaimStatusException("A " + (claim.isDeferred() ? "deferred" : claim.getStatus())
                    + " claim cannot be split");
        }
        if (!Permissions.allows(sheetId, by, Permissions.OPEN, roles, settings)) {
            throw new ClaimStatusException("Your role may not open a claim");
        }
        BigDecimal part;
        try {
            part = new BigDecimal(amount == null ? "" : amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidClaimException("Not an amount: " + amount);
        }
        BigDecimal whole = new BigDecimal(claim.getAmount());
        if (part.scale() > 2 || part.signum() <= 0 || part.compareTo(whole) >= 0) {
            throw new InvalidClaimException("A split needs an amount more than 0.00 and less than " + claim.getAmount());
        }
        claim.setAmount(whole.subtract(part).setScale(2).toPlainString());
        claim.bind(false, null);

        String user = by == null ? "" : by;
        LocalDate date = LocalDate.parse(claim.getDate());
        Person person = people.find(sheetId, claim.getPersonId());
        LocalDate dueBy = date.plusDays(settings.terms(sheetId, person, claim.getDate()));
        Claim created = claims.add(sheetId, claim.getPersonId(), part.setScale(2), date, claim.getNote(),
                null, dueBy, user);
        if (periods.softClosedOn(sheetId, created.getDueBy())) claims.markLateEntry(created);
        created.setStatus(ClaimStatus.OPEN.name());
        created.setNumber(ClaimNumbers.next(created, claims, periods, settings, sheets));
        created.setOpenedBy(user);
        created.setSplitFrom(claim.getNumber());
        split.set(new Split(claim, created));
    }
}
