package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward merges the person into another on the sheet: the person's claims move to the other
 *  (numbers and dates kept, listed after the other's in the order they were added, each losing any
 *  figure a statement bound), their name and aliases become the other's aliases, their patrons are
 *  added to the other's and whoever named them as patron names the other instead (no repeats, never
 *  themselves), their pool memberships (shares added where the other is a member already) and closed
 *  periods' fixed totals move to the other, and the rest of them (statements, holds, watchers,
 *  contact) is dropped with the person. Refused (nothing written) for anyone but a steward, for the
 *  person themselves or someone not on the sheet, and when either of them is retired. */
public class MergePerson {
    public void merge(@RequestParam(name = "by", required = false) String by,
            @Val MergeRequest request, @Val Person person, PersonRepository people,
            ClaimRepository claims, PoolRepository pools, PeriodRepository periods,
            RoleRepository roles, Out<Merge> merged) throws InvalidPersonException {
        String user = by == null ? "" : by;
        String sheetId = person.getSheetId();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, user))) {
            throw new InvalidPersonException("Only a steward can merge people");
        }
        if (request.intoId() == person.getId()) {
            throw new InvalidPersonException("A person cannot be merged into themselves");
        }
        Person into = people.find(sheetId, request.intoId());
        if (into == null) {
            throw new InvalidPersonException("No such person on the sheet: " + request.intoId());
        }
        if (person.isRetired() || into.isRetired()) {
            throw new InvalidPersonException("A retired person cannot be merged");
        }
        claims.moveAll(sheetId, person.getId(), into.getId());
        pools.moveMembers(sheetId, person.getId(), into.getId());
        periods.moveTotals(sheetId, person.getId(), into.getId());
        people.merge(person, into, user);
        merged.set(new Merge(person, into));
    }
}
