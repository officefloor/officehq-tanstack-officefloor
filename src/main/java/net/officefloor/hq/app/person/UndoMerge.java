package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Undoes the last merge into the person (a steward), on the same sheet's today it was made: the merged
 * person is back at the end of the list with the claims that came from them (as they are now, listed
 * again in the order they were) and their own aliases, which leave this person's. Patron, pool, crew,
 * period and statement changes stay. Anyone else, a later day, or a person with no merge to undo is refused.
 */
public class UndoMerge {
    public void undo(@Val Sheet sheet, @Val Person person, PersonRepository people, ClaimRepository claims,
            RoleRepository roles, HttpServletRequest request, Out<Merge> mergeOut)
            throws StewardOnlyException, InvalidPersonException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), Audit.user(request)))) {
            throw new StewardOnlyException("Only a steward may undo a merge");
        }
        LastMerge merge = people.lastMergeInto(person);
        if (merge == null) throw new InvalidPersonException("That person has no merge to undo");
        if (merge.mergedOn() == null || !merge.mergedOn().equals(sheet.today())) {
            throw new InvalidPersonException("A merge can only be undone on the day it was made");
        }
        Set<Long> came = new HashSet<>(people.mergedClaims(merge.id()));
        List<Claim> moving = new ArrayList<>();
        for (Claim claim : claims.findByPerson(sheet.id(), person.getId())) {
            if (came.contains(claim.getId())) moving.add(claim);
        }
        Collections.reverse(moving);
        for (Claim claim : moving) {
            claim.mergeTo(merge.fromId());
            claims.save(claim);
        }
        people.undoMerge(merge, person);
        mergeOut.set(new Merge(people.find(sheet.id(), merge.fromId()), person));
    }
}
