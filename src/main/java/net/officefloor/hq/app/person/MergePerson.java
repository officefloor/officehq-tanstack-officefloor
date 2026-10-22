package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.patron.PatronRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Merge the loaded person into another: only a steward may, into another person on the sheet, neither of them
 *  retired. The merged person's claims move to the other (numbers and dates kept, listed after the other's in the
 *  order they were added, each unbound from any statement); their name and aliases become aliases of the other;
 *  their patrons are added to the other's (not the other themselves, no repeats), and anyone naming them as patron
 *  names the other instead (no repeats); their pool memberships move to the other (shares added when the other is
 *  already a member); their statements, holds, watchers and contact are dropped, and they leave the sheet. */
public class MergePerson {
    public void merge(@Val MergeInput input, @Val Person loaded, PersonRepository people, ClaimRepository claims,
            PatronRepository patrons, PoolRepository pools, WatchRepository watches, RoleRepository roles,
            Out<Merge> merge, Out<PersonResponse> saved) throws InvalidPersonException {
        if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
            throw new InvalidPersonException("Only a steward may merge people.");
        }
        Person into = people.find(loaded.sheetId(), input.intoId());
        if (into == null) {
            throw new InvalidPersonException("No person " + input.intoId() + " on this sheet.");
        }
        if (into.id() == loaded.id()) {
            throw new InvalidPersonException(loaded.name() + " cannot be merged into themselves.");
        }
        people.lock(loaded);
        people.lock(into);
        Person from = people.find(loaded.sheetId(), loaded.id());
        into = people.find(into.sheetId(), into.id());
        if (from == null || into == null) {
            throw new InvalidPersonException("That person is no longer on this sheet.");
        }
        if (from.retiredFrom() != null) {
            throw new InvalidPersonException(from.name() + " is retired and cannot be merged.");
        }
        if (into.retiredFrom() != null) {
            throw new InvalidPersonException(into.name() + " is retired: nobody can be merged into them.");
        }
        claims.moveAll(from.sheetId(), from.id(), into.id());
        patrons.merge(from.sheetId(), from.id(), into.id());
        pools.merge(from.sheetId(), from.id(), into.id());
        watches.drop(from.sheetId(), from.id());
        people.merge(from, into);
        merge.set(new Merge(from.sheetId(), from.name(), into.name()));
        saved.set(PersonResponse.from(people.find(into.sheetId(), into.id())));
    }
}
