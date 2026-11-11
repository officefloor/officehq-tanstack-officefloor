package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Merges the person into another on the sheet (a steward): not themselves, and neither retired. Their
 * claims move to the other (numbers and dates kept, listed after the other's in the order they were
 * added, each keeping any figure a statement bound it to); their statement count is added to the other's
 * (whose last statement stays theirs, or, having none, becomes this person's); their name and aliases become the other's
 * aliases; their patrons, pool and crew memberships (and a crew they lead) go to the other, and anyone naming them as patron names
 * the other instead. Their figures fixed for closed periods (due-totals, carried figures) and their opening
 * balances are added to the other's. Their holds, watchers and contact are dropped, and they leave the list.
 */
public class MergePerson {
    public void merge(@Val MergeInput input, @Val Sheet sheet, @Val Person person, PersonRepository people,
            ClaimRepository claims, PoolRepository pools, CrewRepository crews, PeriodRepository periods, RoleRepository roles, HttpServletRequest request,
            Out<Merge> mergeOut) throws StewardOnlyException, InvalidPersonException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), Audit.user(request)))) {
            throw new StewardOnlyException("Only a steward may merge people");
        }
        Person into = people.find(sheet.id(), input.intoId());
        if (into == null) throw new InvalidPersonException("No such person to merge into");
        if (into.getId().equals(person.getId())) throw new InvalidPersonException("A person cannot be merged into themselves");
        if (person.isRetired() || into.isRetired()) {
            throw new InvalidPersonException("A retired person cannot be merged");
        }
        List<Claim> moving = new ArrayList<>(claims.findByPerson(sheet.id(), person.getId()));
        Collections.reverse(moving);
        for (Claim claim : moving) {
            claim.mergeTo(into.getId());
            claims.save(claim);
        }
        pools.mergeMembers(person.getId(), into.getId());
        crews.mergeMembers(person.getId(), into.getId());
        periods.mergePeople(sheet.id(), person.getId(), into.getId());
        people.merge(person, into);
        mergeOut.set(new Merge(person, into));
    }
}
