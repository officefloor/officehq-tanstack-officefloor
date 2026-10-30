package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimNumbering;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/**
 * Transfers the person to another sheet (a steward only; never a retired person, never to this sheet, never onto an
 * archived sheet, nor when their contact is already on the other sheet, or their name or an alias on another sheet of its
 * tree). When their name or an alias is already on the other sheet (as names are compared) they are merged into that
 * person there, exactly as a merge does: their claims listed after the other's, their name and aliases becoming the
 * other's aliases, figures a statement bound lost; their grades, terms and contact are dropped (never into a retired
 * person, nor when their names are held by more than one person there). Otherwise they arrive with
 * every grade they were given, their terms, contact and aliases, and their claims (status, dates, notes, tags,
 * settled figures and logs kept, listed in the same order); each claim that had a number takes the other sheet's
 * next, in the order the claims were opened (by the sheet's today when opened, then the order added). Figures a
 * statement bound and the claims' references are not carried; the statement count starts at 0. They leave this
 * sheet entirely: patron links, pool memberships, holds, statements and watches are dropped.
 */
public class TransferPerson {
    public void transfer(@Val TransferRequest request, @Val Person person, People people, Claims claims,
            ClaimNumbering numbering, Sheets sheets, Settings settings, Audit audit, @Val RollupFigures before,
            Rollup rollup) throws InvalidPersonException {
        String sheetId = person.getSheetId();
        String to = request.toSheet();
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new InvalidPersonException("Only a steward can transfer people");
        }
        if (person.getRetiredFrom() != null) {
            throw new InvalidPersonException("A retired person cannot be transferred");
        }
        if (sheets.archived(to)) {
            throw new InvalidPersonException("The sheet transferred to is archived");
        }
        List<String> names = new ArrayList<>();
        names.add(person.getName());
        names.addAll(person.getAliases());
        Person into = null;
        for (String name : names) {
            if (people.nameUsedInTree(to, name, person)) {
                throw new InvalidPersonException("The name " + name + " is already on another sheet of the other sheet's tree");
            }
            Person holder = people.findByName(to, name);
            if (holder != null) {
                if (into != null && into.getId() != holder.getId()) {
                    throw new InvalidPersonException("The names are held by more than one person on the other sheet");
                }
                into = holder;
            }
        }
        if (into != null && into.getRetiredFrom() != null) {
            throw new InvalidPersonException("A retired person cannot be merged");
        }
        if (into == null && person.getContact() != null && !person.getContact().isBlank()
                && people.contactTaken(to, person.getContact(), null)) {
            throw new InvalidPersonException("The contact is already on the other sheet");
        }
        RollupFigures toBefore = rollup.figures(to);

        Person arrived = into == null ? people.transferIn(person, to) : people.transferInto(person, into);
        // Claims in the order they are listed (oldest listed first), so they keep their order.
        List<Claim> moved = new ArrayList<>(claims.ofPerson(sheetId, person.getId()));
        java.util.Collections.reverse(moved);
        Map<Long, Long> newIds = new HashMap<>();
        for (Claim claim : moved) {
            newIds.put(claim.getId(), claims.transferIn(claim, to, arrived.getId()));
        }
        // Numbers in the order the claims were opened.
        List<Claim> numbered = new ArrayList<>(moved.stream().filter(c -> c.getNumber() != null).toList());
        Map<Long, LocalDate> openedOn = new HashMap<>();
        for (Claim claim : numbered) {
            openedOn.put(claim.getId(), claims.openedOn(claim));
        }
        numbered.sort(Comparator.comparing((Claim c) -> openedOn.get(c.getId()), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingLong(Claim::getId));
        Map<String, String> renumbered = new HashMap<>();
        for (Claim claim : numbered) {
            String number = numbering.next(to, claim.getDate());
            renumbered.put(claim.getNumber(), number);
            claims.numberTransferred(to, newIds.get(claim.getId()), number);
        }
        for (Claim claim : moved) {
            claims.correctsTransferred(to, newIds.get(claim.getId()),
                    claim.getCorrects() == null ? null : renumbered.get(claim.getCorrects()));
        }

        for (Claim claim : moved) {
            claims.remove(claim);
        }
        people.transferOut(person);

        audit.recordOnCommit("PERSON_TRANSFERRED sheet=" + to + " by=" + request.user()
                + " name=" + person.getName() + " from=" + sheetId);
        if (into != null) {
            audit.recordOnCommit("PERSON_MERGED sheet=" + to + " by=" + request.user()
                    + " from=" + person.getName() + " into=" + into.getName());
        }
        rollup.recordChanges(to, request.user(), toBefore);
        audit.recordOnCommit("PERSON_TRANSFERRED sheet=" + sheetId + " by=" + request.user()
                + " name=" + person.getName() + " to=" + to);
        rollup.recordChanges(sheetId, request.user(), before);
    }
}
