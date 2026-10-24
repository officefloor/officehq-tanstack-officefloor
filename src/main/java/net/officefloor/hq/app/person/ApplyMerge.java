package net.officefloor.hq.app.person;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClerkLimitException;
import net.officefloor.hq.app.plan.Plan;
import net.officefloor.hq.app.plan.PlanRepository;
import net.officefloor.hq.app.crew.Crew;
import net.officefloor.hq.app.crew.CrewMember;
import net.officefloor.hq.app.crew.CrewMemberRepository;
import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.pool.PoolMember;
import net.officefloor.hq.app.pool.PoolMemberRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.BindingLogRepository;
import net.officefloor.hq.app.statement.BindingLogs;
import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Merges the person into another of the sheet; only a steward may, and not into themselves, nor when either of them is
 * retired. Their claims move to the other, keeping their numbers and dates, listed after the other's claims in the
 * order they were added, each keeping any figure a statement bound it to. Their name and aliases become aliases of the
 * other. Their patrons are added to the other's (not the other themselves, no repeats); anyone naming them as patron
 * names the other instead (not themselves, no repeats). Their pool memberships move to the other (shares added when the
 * other is already a member). The crews they lead are led by the other (who leaves those crews' members), and their crew
 * memberships move to the other (dropped where the other already leads or is in the crew). Their plans become the other's. Their statements become the other's, standing before the other's own, so the other's
 * last statement stays theirs (or, having none, becomes their last). Their holds, dated grades, watchers and contact
 * are dropped, and they leave the sheet.
 */
public class ApplyMerge {
    public void apply(@Val MergeInput input, @Val Person person, PersonRepository people, ClaimRepository claims,
            PersonAliasRepository aliases, PatronRepository patrons, PoolMemberRepository members,
            StatementRepository statements, BindingLogRepository bindings, SheetRepository sheets, WatchRepository watches, PersonGradeRepository grades,
            PlanRepository plans, CrewRepository crews, CrewMemberRepository crewMembers,
            RoleRepository roles, EntityManager entityManager,
            @RequestHeader(name = "X-User", required = false) String userHeader, Out<PersonMerged> merged)
            throws InvalidPersonException, ClerkLimitException {
        String sheetId = person.getSheetId();
        String user = AuditUser.decode(userHeader);
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, user))) {
            throw new ClerkLimitException("Only a steward can merge people.");
        }
        claims.lockSheet(sheetId);
        entityManager.refresh(person);
        Person into = people.findByIdAndSheetId(input.intoId(), sheetId)
                .orElseThrow(() -> new InvalidPersonException("No such person on the sheet."));
        if (into.getId().equals(person.getId())) {
            throw new InvalidPersonException("A person cannot be merged into themselves.");
        }
        if (person.getRetiredFrom() != null || into.getRetiredFrom() != null) {
            throw new InvalidPersonException("A retired person cannot be merged.");
        }
        Long from = person.getId();
        Long to = into.getId();

        // Claims: moved in the order they were added, each listed as the other's newest.
        List<Claim> moved = new ArrayList<>(claims.findBySheetIdOrderByIdDesc(sheetId).stream()
                .filter(c -> from.equals(c.getPersonId())).toList());
        moved.sort(Comparator.comparing(Claim::getId));
        for (Claim claim : moved) {
            claim.setPersonId(to);
            claims.saveAndFlush(claim);
            claims.relist(claim.getId());
        }

        // Plans move to the other.
        for (Plan plan : plans.findBySheetIdOrderByIdAsc(sheetId)) {
            if (from.equals(plan.getPersonId())) {
                plan.setPersonId(to);
                plans.saveAndFlush(plan);
            }
        }

        // Name and aliases become the other's aliases.
        List<PersonAlias> theirAliases = aliases.findByPersonIdOrderByIdAsc(from);
        aliases.deleteAll(theirAliases);
        aliases.flush();
        aliases.save(new PersonAlias(sheetId, to, person.getName()));
        theirAliases.forEach(a -> aliases.save(new PersonAlias(sheetId, to, a.getAlias())));

        // Patrons: theirs added to the other's; those naming them name the other instead.
        for (Patron patron : patrons.findByPersonIdOrderByIdAsc(from)) {
            patrons.delete(patron);
            patrons.flush();
            if (!patron.getPatronId().equals(to) && !patrons.existsByPersonIdAndPatronId(to, patron.getPatronId())) {
                patrons.saveAndFlush(new Patron(sheetId, to, patron.getPatronId()));
            }
        }
        for (Patron naming : patrons.findByPatronIdOrderByIdAsc(from)) {
            patrons.delete(naming);
            patrons.flush();
            if (!naming.getPersonId().equals(to) && !patrons.existsByPersonIdAndPatronId(naming.getPersonId(), to)) {
                patrons.saveAndFlush(new Patron(sheetId, naming.getPersonId(), to));
            }
        }

        // Pool memberships move, adding shares where the other is already a member.
        for (PoolMember member : members.findByPersonIdOrderByIdAsc(from)) {
            var already = members.findByPoolIdAndPersonId(member.getPoolId(), to);
            if (already.isPresent()) {
                already.get().setShare(already.get().getShare().add(member.getShare()));
                members.delete(member);
                members.flush();
                members.saveAndFlush(already.get());
            } else {
                member.setPersonId(to);
                members.saveAndFlush(member);
            }
        }

        // Crews they lead are led by the other; their crew memberships move to the other where not already there.
        for (Crew crew : crews.findByLeadIdOrderByIdAsc(from)) {
            crew.setLeadId(to);
            crews.saveAndFlush(crew);
        }
        for (CrewMember member : crewMembers.findByPersonIdOrderByIdAsc(from)) {
            Crew crew = crews.findById(member.getCrewId()).orElse(null);
            if (crew == null || to.equals(crew.getLeadId())
                    || crewMembers.existsByCrewIdAndPersonId(member.getCrewId(), to)) {
                crewMembers.delete(member);
            } else {
                member.setPersonId(to);
                crewMembers.save(member);
            }
        }
        crewMembers.flush();
        for (CrewMember member : crewMembers.findByPersonIdOrderByIdAsc(to)) {
            if (crews.findById(member.getCrewId()).map(c -> to.equals(c.getLeadId())).orElse(false)) {
                crewMembers.delete(member);
            }
        }
        crewMembers.flush();

        // Statements move to the other, before their own; the claims bound now are recorded as the other's binding.
        statements.moveAll(from, sheetId, to);
        BindingLogs.record(bindings, claims, sheets, sheetId, to);

        // Dropped: watchers, dated grades; holds and contact go with the person.
        watches.deleteAll(watches.findByPersonIdOrderByIdAsc(from));
        grades.deleteAll(grades.findByPersonIdOrderByFromDateAsc(from));
        statements.flush();
        people.delete(person);
        people.flush();
        entityManager.refresh(into);
        merged.set(new PersonMerged(person.getName(), into));
    }
}
