package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.pool.PoolStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Merges the person into another on the sheet, refusing the same person or either of them retired: their claims,
 *  name and aliases, patrons, the patronage of them and their pool memberships move to the other; the rest of
 *  them is dropped. */
public class MergePerson {
    public void merge(@Val MergeRequest request, @Val Person person, PersonStore people, ClaimStore claims,
            PatronStore patrons, PoolStore pools, Out<MergedInto> merged) throws InvalidSheetException {
        Person into = request.into() == null ? null : people.find(person.getSheetId(), request.into());
        if (into == null) {
            throw new InvalidSheetException("No such person to merge into");
        }
        if (into.getId() == person.getId()) {
            throw new InvalidSheetException("A person cannot be merged into themselves");
        }
        if (person.isRetired() || into.isRetired()) {
            throw new InvalidSheetException("A retired person cannot be merged");
        }
        String sheetId = person.getSheetId();
        claims.moveAll(sheetId, person.getId(), into.getId());
        patrons.merge(sheetId, person.getId(), into.getId());
        pools.merge(sheetId, person.getId(), into.getId());
        people.mergeInto(person, into);
        merged.set(new MergedInto(people.find(sheetId, into.getId())));
    }
}
