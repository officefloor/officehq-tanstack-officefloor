package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/**
 * Undoes the last merge into the person (a steward only, the one who made it, on the sheet's today it was made): the
 * merged person is back at the end of the list with the claims that came from them (their numbers and status as they
 * are now) and their own aliases, which leave the other's; patron and pool changes stay. Writes PERSON_MERGE_UNDONE.
 */
public class UndoMergePerson {
    public void undo(@Val MergeUndoRequest request, @Val Person person, People people, Claims claims, Sheets sheets,
            Settings settings, Audit audit, @Val RollupFigures before, Rollup rollup) throws InvalidPersonException {
        String sheetId = person.getSheetId();
        String user = request.user();
        if (!Settings.STEWARD.equals(settings.role(sheetId, user))) {
            throw new InvalidPersonException("Only a steward can undo a merge");
        }
        People.Merge merge = people.lastMergeInto(person);
        if (merge == null) {
            throw new InvalidPersonException("No merge into the person to undo");
        }
        if (!java.util.Objects.equals(merge.mergedBy(), user)) {
            throw new InvalidPersonException("Only the one who merged can undo the merge");
        }
        if (!sheets.today(sheetId).equals(merge.mergedOn())) {
            throw new InvalidPersonException("A merge can be undone only on the day it was made");
        }
        java.util.List<People.MergedClaim> moved = people.mergedClaims(person, merge);
        Person restored = people.undoMerge(person, merge);
        if (restored == null) {
            throw new InvalidPersonException("The merged person cannot be restored");
        }
        for (People.MergedClaim claim : moved) {
            claims.unmerge(sheetId, claim.claimId(), restored.getId(), claim.listedSeq());
        }
        audit.recordOnCommit("PERSON_MERGE_UNDONE sheet=" + sheetId + " by=" + user
                + " name=" + restored.getName() + " from=" + person.getName());
        rollup.recordChanges(sheetId, user, before);
    }
}
