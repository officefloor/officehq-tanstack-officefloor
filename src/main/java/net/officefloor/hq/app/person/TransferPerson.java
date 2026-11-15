package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimNumbers;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward transfers the person to another sheet: they arrive with every grade given them (and its
 *  from-date), their terms, contact and aliases, and their claims with their status, dates, notes,
 *  tags, frozen figures, logs and comments; each claim that had a number gets the other sheet's next,
 *  in the order the claims were opened (by the sheet's today then, then the order they were added).
 *  Figures a statement bound are lost, their statement count starts at 0, and references, holds and
 *  settle batches are not carried. They leave this sheet entirely: patron links (either way), pool
 *  memberships, watches and closed periods' fixed totals are dropped. Refused (nothing written) for
 *  anyone but a steward, a retired person, this sheet's own id or an archived sheet, and when their
 *  name, an alias or their contact is already on the other sheet. */
public class TransferPerson {
    public void transfer(@RequestParam(name = "by", required = false) String by,
            @Val TransferRequest request, @Val Person person, PersonRepository people,
            ClaimRepository claims, PeriodRepository periods, RateRepository rates,
            SettingRepository settings, SheetRepository sheets, RoleRepository roles,
            Out<Transfer> transferred) throws InvalidPersonException {
        String user = by == null ? "" : by;
        String sheetId = person.getSheetId();
        String to = request.to();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, user))) {
            throw new InvalidPersonException("Only a steward can transfer people");
        }
        if (to.equals(sheetId)) {
            throw new InvalidPersonException("A person cannot be transferred to their own sheet");
        }
        if (person.isRetired()) {
            throw new InvalidPersonException("A retired person cannot be transferred");
        }
        if (sheets.isArchived(to)) {
            throw new InvalidPersonException("The sheet is archived: " + to);
        }
        if (people.nameTaken(to, person.getName(), null)
                || person.getAliases().stream().anyMatch((a) -> people.nameTaken(to, a, null))
                || (person.getContact() != null && people.contactTaken(to, person.getContact(), null))) {
            throw new InvalidPersonException("The name, an alias or the contact is already on " + to);
        }
        sheets.open(to, user);
        Rollup before = Rollup.of(to, people, claims, rates, sheets, settings);
        for (long id : claims.transferAll(sheetId, person.getId(), to)) {
            Claim claim = claims.find(to, id);
            claims.renumber(claim, ClaimNumbers.next(claim, claims, periods, settings, sheets));
        }
        people.transfer(person, to);
        transferred.set(new Transfer(person, to, before));
    }
}
