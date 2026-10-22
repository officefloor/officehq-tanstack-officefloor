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
 *  one (created when never loaded) that is not archived. When one person there has their name or one of their aliases (as names are compared; more
 *  than one is refused), they are merged into that person as MergePerson merges (not retired; their claims after
 *  theirs, their name and aliases joined to theirs, their contact and figures bound to statements dropped);
 *  otherwise the other sheet must not have their contact. They arrive
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
        java.util.Set<Long> named = new java.util.LinkedHashSet<>();
        for (String name : java.util.stream.Stream.concat(java.util.stream.Stream.of(person.name()),
                person.aliases().stream()).toList()) {
            Person there = people.findByName(to, name);
            if (there != null) {
                named.add(there.id());
            }
        }
        if (named.size() > 1) {
            throw new InvalidPersonException("Sheet " + to + " has more than one person of the names of "
                    + person.name() + ".");
        }
        Person into = named.isEmpty() ? null : people.find(to, named.iterator().next());
        if (into != null) {
            people.lock(into);
            into = people.find(to, into.id());
            if (into.retiredFrom() != null) {
                throw new InvalidPersonException(into.name() + " is retired: nobody can be merged into them.");
            }
        }
        if (into == null && person.contact() != null && people.contactInUse(to, person.contact(), null)) {
            throw new InvalidPersonException("Sheet " + to + " already has someone with the contact "
                    + person.contact() + ".");
        }
        var toBefore = rollups.take(to);
        patrons.drop(from, person.id());
        pools.drop(from, person.id());
        watches.drop(from, person.id());
        claims.transfer(from, person.id(), to);
        people.transfer(person, to);
        if (into != null) {
            claims.moveAll(to, person.id(), into.id());
            people.merge(people.find(to, person.id()), into);
        }
        transfer.set(new Transfer(from, to, person.name(), toBefore, into != null ? into.name() : null));
        saved.set(PersonResponse.from(people.find(to, into != null ? into.id() : person.id())));
    }
}
