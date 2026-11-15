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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A steward transfers the person to another sheet: they arrive with every grade given them (and its
 *  from-date), their terms, contact and aliases, and their claims with their status, dates, notes,
 *  tags, frozen figures, the figures a statement bound them to, logs and comments; each claim that had a number gets the other sheet's next,
 *  in the order the claims were opened (by the sheet's today then, then the order they were added).
 *  Their statement count starts at 0, and references, holds and
 *  settle batches are not carried. They leave this sheet entirely: patron links (either way), pool
 *  memberships, watches and closed periods' fixed totals are dropped. When someone on the other sheet
 *  already has their name or an alias (as names are compared), they are merged into that person
 *  there, as a merge does (see MergePerson): their claims after theirs, their statement count added
 *  to theirs (theirs keeping their last statement), their name and aliases joined to theirs, the rest of them (contact included) dropped. Refused (nothing written) for anyone but a
 *  steward, a retired person, this sheet's own id or an archived sheet, when their names match more
 *  than one person there or a retired one, and (when not merged) when their contact is already there. */
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
        Set<Long> matched = new LinkedHashSet<>();
        for (String name : names(person)) {
            Person match = people.findByName(to, name);
            if (match != null) {
                matched.add(match.getId());
            }
        }
        if (matched.size() > 1) {
            throw new InvalidPersonException("The name and aliases match more than one person on " + to);
        }
        Person into = matched.isEmpty() ? null : people.find(to, matched.iterator().next());
        if (into != null && into.isRetired()) {
            throw new InvalidPersonException("A retired person cannot be merged");
        }
        if (into == null && person.getContact() != null && people.contactTaken(to, person.getContact(), null)) {
            throw new InvalidPersonException("The contact is already on " + to);
        }
        sheets.open(to, user);
        Rollup before = Rollup.of(to, people, claims, rates, sheets, settings);
        for (long id : claims.transferAll(sheetId, person.getId(), to)) {
            Claim claim = claims.find(to, id);
            claims.renumber(claim, ClaimNumbers.next(claim, claims, periods, settings, sheets));
        }
        people.transfer(person, to, into != null);
        if (into != null) {
            Person arrived = people.find(to, person.getId());
            arrived.setName(person.getName()); // arrived under a stand-in, as the name is taken there
            claims.moveAll(to, arrived.getId(), into.getId());
            people.merge(arrived, into, user);
        }
        transferred.set(new Transfer(person, to, before, into));
    }

    private static List<String> names(Person person) {
        List<String> names = new ArrayList<>();
        names.add(person.getName());
        names.addAll(person.getAliases());
        return names;
    }
}
