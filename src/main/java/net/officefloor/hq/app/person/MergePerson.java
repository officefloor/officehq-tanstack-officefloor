package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;

/**
 * Merges the person into another (a steward only; never into themselves, nor when either is retired). Their claims
 * move to the other, numbers and dates kept, listed after the other's in the order they were added, each keeping
 * the figure a statement bound it to; their statement count adds to the other's, whose last statement stays theirs
 * (or, having none, becomes the merged person's); their name and aliases become the other's aliases; their patrons are added to the other's
 * and whoever named them as patron names the other instead (never themselves, never twice); their pool memberships
 * move to the other (shares added where the other is already a member). Their holds, watchers and
 * contact are dropped and they leave the sheet.
 */
public class MergePerson {
    public void merge(@Val MergeRequest request, @Val Person person, People people, Claims claims, Settings settings,
            Audit audit, @Val RollupFigures before, Rollup rollup) throws InvalidPersonException {
        String sheetId = person.getSheetId();
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new InvalidPersonException("Only a steward can merge people");
        }
        Person into = null;
        try {
            into = people.find(sheetId, Long.parseLong(request.intoId().trim()));
        } catch (NumberFormatException e) {
            // not an id this app assigned
        }
        if (into == null) {
            throw new InvalidPersonException("The person to merge into is not on the sheet");
        }
        if (into.getId() == person.getId()) {
            throw new InvalidPersonException("A person cannot be merged into themselves");
        }
        if (person.getRetiredFrom() != null || into.getRetiredFrom() != null) {
            throw new InvalidPersonException("A retired person cannot be merged");
        }
        java.util.List<Claim> moved = new java.util.ArrayList<>(claims.ofPerson(sheetId, person.getId()));
        moved.sort(java.util.Comparator.comparingLong(Claim::getId));
        for (Claim claim : moved) {
            claims.merge(claim, into.getId());
        }
        people.merge(person, into);
        audit.recordOnCommit("PERSON_MERGED sheet=" + sheetId + " by=" + request.user()
                + " from=" + person.getName() + " into=" + into.getName());
        rollup.recordChanges(sheetId, request.user(), before);
    }
}
