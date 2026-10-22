package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.patron.PatronRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Transfer the loaded person to another sheet: only a steward may, a person not retired, to a sheet other than this
 *  one (created when never loaded) that is not archived and has none of their name, aliases or contact. They arrive
 *  with every grade they were given (and its from-date), their terms, contact and aliases, and their claims (status,
 *  dates, notes, settled dues and logs kept; each claim that had a number takes the next number of the other sheet,
 *  in the order the claims were added; unbound from any statement; references dropped), with no statement taken.
 *  They leave this sheet entirely: their patron links, pool memberships, holds, statements and watchers are dropped. */
public class TransferPerson {
    public void transfer(@Val TransferInput input, @Val Person loaded, PersonRepository people, ClaimRepository claims,
            PatronRepository patrons, PoolRepository pools, WatchRepository watches, RoleRepository roles,
            SheetRepository sheets, Rollups rollups, Out<Transfer> transfer, Out<PersonResponse> saved)
            throws InvalidPersonException {
        String from = loaded.sheetId();
        String to = input.toSheet();
        if (!Role.STEWARD.equals(roles.of(from, input.user()))) {
            throw new InvalidPersonException("Only a steward may transfer people.");
        }
        if (to.equals(from)) {
            throw new InvalidPersonException(loaded.name() + " is already on this sheet.");
        }
        people.lock(loaded);
        Person person = people.find(from, loaded.id());
        if (person == null) {
            throw new InvalidPersonException("That person is no longer on this sheet.");
        }
        if (person.retiredFrom() != null) {
            throw new InvalidPersonException(person.name() + " is retired and cannot be transferred.");
        }
        sheets.ensure(to, input.user());
        if (sheets.archived(to)) {
            throw new InvalidPersonException("Sheet " + to + " is archived: nobody can be transferred to it.");
        }
        if (people.nameInUse(to, person.name(), null)) {
            throw new InvalidPersonException("Sheet " + to + " already has the name " + person.name() + ".");
        }
        for (String alias : person.aliases()) {
            if (people.nameInUse(to, alias, null)) {
                throw new InvalidPersonException("Sheet " + to + " already has the name " + alias + ".");
            }
        }
        if (person.contact() != null && people.contactInUse(to, person.contact(), null)) {
            throw new InvalidPersonException("Sheet " + to + " already has someone with the contact "
                    + person.contact() + ".");
        }
        var toBefore = rollups.take(to);
        patrons.drop(from, person.id());
        pools.drop(from, person.id());
        watches.drop(from, person.id());
        claims.transfer(from, person.id(), to);
        people.transfer(person, to);
        transfer.set(new Transfer(from, to, person.name(), toBefore));
        saved.set(PersonResponse.from(people.find(to, person.id())));
    }
}
