package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** The steward who made the last merge into the person undoes it, on the sheet's today it was made:
 *  the merged person is back at the end of the list (as they were merged, with their grades and the
 *  rest of their own) with the claims that came from them, as those claims are now, and their own
 *  aliases, which leave the other's, as does the alias their name became. Patron, pool, period total,
 *  statement, plan and crew changes the merge made stay. Refused (nothing written) for anyone but who
 *  made the merge, any later day, and a person with no merge to undo (none, already undone, or made as
 *  a person arrived by transfer). */
public class UndoMerge {
    public void undo(@RequestParam(name = "by", required = false) String by, @Val Person person,
            PersonRepository people, ClaimRepository claims, SheetRepository sheets,
            Out<MergeUndone> undone) throws InvalidPersonException {
        String user = by == null ? "" : by;
        String sheetId = person.getSheetId();
        PersonMerge merge = people.lastMergeInto(sheetId, person.getId());
        if (merge == null || merge.undone() || !merge.undoable()) {
            throw new InvalidPersonException("The person has no merge to undo");
        }
        if (!merge.mergedBy().equals(user)) {
            throw new InvalidPersonException("Only who made the merge can undo it");
        }
        if (!sheets.today(sheetId).equals(merge.mergedOn())) {
            throw new InvalidPersonException("A merge can be undone only on the day it was made");
        }
        people.undoMerge(merge);
        claims.moveBack(sheetId, people.mergedClaims(merge), person.getId(), merge.fromId());
        undone.set(new MergeUndone(people.find(sheetId, merge.fromId()), people.find(sheetId, person.getId())));
    }
}
