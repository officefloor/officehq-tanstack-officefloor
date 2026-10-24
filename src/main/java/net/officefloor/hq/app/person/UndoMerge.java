package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Undo the last merge into the loaded person: only the steward who made it, on the sheet-today it was made. The
 *  merged person is back at the end of the list as they were (grades, terms, holds, statements), with the claims that
 *  came from them (as they are now) and their own aliases, which leave the other's along with their name; patron and
 *  pool changes stay. */
public class UndoMerge {
    public void undo(@Val MergeUndoRequest request, @Val Person loaded, PersonRepository people,
            PersonMergeRepository merges, ClaimRepository claims, RoleRepository roles, SheetRepository sheets,
            Out<MergeUndone> undone, Out<PersonResponse> saved) throws InvalidPersonException {
        String sheetId = loaded.sheetId();
        if (!Role.STEWARD.equals(roles.of(sheetId, request.user()))) {
            throw new InvalidPersonException("Only a steward may undo a merge.");
        }
        people.lock(loaded);
        PersonMergeRepository.Made merge = merges.last(sheetId, loaded.id());
        if (merge == null) {
            throw new InvalidPersonException("Nobody has been merged into " + loaded.name() + " to undo.");
        }
        if (!merge.madeBy().equals(request.user())) {
            throw new InvalidPersonException("Only " + merge.madeBy() + ", who merged " + merge.fromName() + " into "
                    + loaded.name() + ", may undo it.");
        }
        if (merge.madeOn() == null || !merge.madeOn().equals(sheets.today(sheetId))) {
            throw new InvalidPersonException("The merge of " + merge.fromName() + " can be undone only on the day it "
                    + "was made (" + merge.madeOn() + ").");
        }
        Person into = people.find(sheetId, loaded.id());
        merges.restoreRows(merge);
        Person from = people.find(sheetId, merge.fromId());
        merges.restoreAliases(merge);
        merges.restoreStatements(merge, from, into);
        claims.moveBack(sheetId, merges.claims(merge.id()), into.id(), from.id());
        merges.undone(merge);
        undone.set(new MergeUndone(sheetId, from.name(), into.name()));
        saved.set(PersonResponse.from(people.find(sheetId, from.id())));
    }
}
