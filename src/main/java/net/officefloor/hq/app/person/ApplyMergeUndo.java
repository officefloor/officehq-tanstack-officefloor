package net.officefloor.hq.app.person;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClerkLimitException;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.BindingLogRepository;
import net.officefloor.hq.app.statement.BindingLogs;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Undoes the last merge into the person: only by the steward who made it, on the sheet's today it was made. The merged
 * person is back, under their own id, at the end of the list, with the claims that came from them (as they are now)
 * and their own aliases, which leave the other's. Patron, pool, crew, plan and statement changes stay, as do the
 * holds, dated grades, watchers and contact the merge dropped. Refused when their name or an alias of theirs is now
 * someone else's.
 */
public class ApplyMergeUndo {
    public void apply(@Val Person into, @Val Sheet sheet, PersonRepository people, ClaimRepository claims,
            PersonAliasRepository aliases, PersonMergeRepository merges, BindingLogRepository bindings,
            SheetRepository sheets, RoleRepository roles, History history, EntityManager entityManager,
            @RequestHeader(name = "X-User", required = false) String userHeader, Out<PersonMergeUndone> undone)
            throws InvalidPersonException, ClerkLimitException {
        String sheetId = into.getSheetId();
        String user = AuditUser.decode(userHeader);
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, user))) {
            throw new ClerkLimitException("Only a steward can undo a merge.");
        }
        claims.lockSheet(sheetId);
        entityManager.refresh(into);
        PersonMerge merge = merges.findFirstBySheetIdAndIntoIdOrderByIdDesc(sheetId, into.getId())
                .orElseThrow(() -> new InvalidPersonException("There is no merge into the person to undo."));
        if (!merge.getMadeOn().equals(sheet.getToday())) {
            throw new InvalidPersonException("A merge can only be undone on the day it was made.");
        }
        if (!merge.getMadeBy().equals(user)) {
            throw new InvalidPersonException("Only the steward who made the merge can undo it.");
        }
        Person was = history.read(merge.getState(), Person.class);

        // The aliases the merge gave the other that they still have leave them; the names coming back must be free.
        Set<Long> given = new HashSet<>(merge.getAliasIds());
        List<PersonAlias> leaving = aliases.findByPersonIdOrderByIdAsc(into.getId()).stream()
                .filter(a -> given.contains(a.getId())).toList();
        Set<Long> leavingIds = new HashSet<>(leaving.stream().map(PersonAlias::getId).toList());
        Set<String> taken = new HashSet<>();
        people.findListed(sheetId).forEach(p -> taken.add(p.getName().toLowerCase(Locale.ROOT)));
        aliases.findBySheetIdOrderByIdAsc(sheetId).stream().filter(a -> !leavingIds.contains(a.getId()))
                .forEach(a -> taken.add(a.getAlias().toLowerCase(Locale.ROOT)));
        List<String> names = new ArrayList<>(merge.getAliases());
        names.add(was.getName());
        for (String name : names) {
            if (taken.contains(name.toLowerCase(Locale.ROOT))) {
                throw new InvalidPersonException("The name " + name + " is now someone else's.");
            }
        }
        aliases.deleteAll(leaving);
        aliases.flush();

        // They are back under their own id, at the end of the list.
        long end = people.findListed(sheetId).stream().mapToLong(Person::getPlace).max().orElse(0) + 1;
        merges.restore(was);
        entityManager.flush();
        Person restored = people.findById(was.getId()).orElseThrow();
        entityManager.refresh(restored);
        restored.getDatedTerms().addAll(was.getDatedTerms());
        restored.getRetirements().addAll(was.getRetirements());
        restored.getSnapshots().addAll(was.getSnapshots());
        restored.placeAt(Math.max(end, restored.getId()));
        people.saveAndFlush(restored);
        merge.getAliases().forEach(a -> aliases.save(new PersonAlias(sheetId, restored.getId(), a)));
        aliases.flush();

        // The claims that came from them and are still the other's go back, in the order they were added.
        boolean moved = false;
        for (Long claimId : merge.getClaimIds()) {
            Claim claim = claims.findByIdAndSheetId(claimId, sheetId).orElse(null);
            if (claim != null && into.getId().equals(claim.getPersonId())) {
                claim.setPersonId(restored.getId());
                claims.saveAndFlush(claim);
                claims.relist(claim.getId());
                moved = true;
            }
        }
        if (moved) {
            BindingLogs.record(bindings, claims, sheets, sheetId, into.getId());
            BindingLogs.record(bindings, claims, sheets, sheetId, restored.getId());
        }

        merges.delete(merge);
        merges.flush();
        entityManager.refresh(into);
        undone.set(new PersonMergeUndone(restored, into));
    }
}
