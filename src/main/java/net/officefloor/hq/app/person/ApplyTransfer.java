package net.officefloor.hq.app.person;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimComment;
import net.officefloor.hq.app.claim.ClaimCommentRepository;
import net.officefloor.hq.app.claim.ClaimLine;
import net.officefloor.hq.app.claim.ClaimLineRepository;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClerkLimitException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.crew.Crew;
import net.officefloor.hq.app.crew.CrewMemberRepository;
import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.pool.PoolMemberRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.BindingLogRepository;
import net.officefloor.hq.app.statement.BindingLogs;
import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Transfers the person to another sheet by id; only a steward may, not a retired person, not to this sheet itself,
 * nor to an archived sheet, nor when their contact is already on the other sheet. When their name or an alias of theirs
 * is already someone's name or alias there (ignoring case), they are merged into that person instead, exactly as a
 * merge does (not into a retired person): their claims are listed after that person's, each keeping any figure a
 * statement bound it to, their statements become that person's (before that person's own), their name and aliases become that person's aliases, and their grades, terms and contact are
 * dropped. Otherwise they arrive there with every grade they were given
 * (and its from-date), their terms, contact and aliases, and their claims with their status, dates, notes, tags,
 * frozen dues, the figures a statement bound them to, status logs, comments and lines, listed in the same order, and their
 * statements. Each claim that had a number takes the other
 * sheet's next one, in the order they were opened (by the sheet's today when each was opened, then by the order they
 * were added). References are not carried.
 * Their plans go with them, each keeping the claims it made. They leave this sheet entirely: their claims, holds, patron links (both ways), pool memberships,
 * crew memberships and watchers are dropped; the crews they lead keep their members and have no lead.
 */
public class ApplyTransfer {
    public void apply(@Val TransferInput input, @Val Person person, PersonRepository people, ClaimRepository claims,
            ClaimLogRepository logs, ClaimCommentRepository comments, ClaimLineRepository claimLines,
            PersonAliasRepository aliases,
            PatronRepository patrons, PoolMemberRepository members, StatementRepository statements, BindingLogRepository bindings,
            WatchRepository watches, PersonGradeRepository grades, RoleRepository roles, SheetRepository sheets,
            RateRepository rates, PeriodRepository periods, CrewRepository crews, CrewMemberRepository crewMembers,
            net.officefloor.hq.app.plan.PlanRepository plans, net.officefloor.hq.app.plan.PlanClaimRepository planClaims,
            net.officefloor.hq.app.plan.PlanPauseRepository planPauses,
            net.officefloor.hq.app.plan.PlanChangeRepository planChanges,
            EntityManager entityManager,
            @RequestHeader(name = "X-User", required = false) String userHeader, Out<PersonTransferred> transferred)
            throws InvalidPersonException, ClerkLimitException {
        String sheetId = person.getSheetId();
        String user = AuditUser.decode(userHeader);
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, user))) {
            throw new ClerkLimitException("Only a steward can transfer people.");
        }
        String to = input.toSheet();
        if (to.equals(sheetId)) {
            throw new InvalidPersonException("A person cannot be transferred to their own sheet.");
        }
        claims.lockSheet(sheetId);
        entityManager.refresh(person);
        if (person.getRetiredFrom() != null) {
            throw new InvalidPersonException("A retired person cannot be transferred.");
        }
        Sheet target = sheets.findById(to).orElse(null);
        if (target != null && target.isArchived()) {
            throw new InvalidPersonException("The other sheet is archived.");
        }
        Long from = person.getId();
        List<PersonAlias> theirAliases = aliases.findByPersonIdOrderByIdAsc(from);
        List<String> names = new ArrayList<>();
        names.add(person.getName());
        theirAliases.forEach(a -> names.add(a.getAlias()));
        List<Person> onTarget = people.findListed(to);
        Person into = null;
        for (String name : names) {
            into = PersonAliasRepository.findByNameOrAlias(onTarget, aliases, to, name).orElse(null);
            if (into != null) {
                break;
            }
        }
        if (into != null && into.getRetiredFrom() != null) {
            throw new InvalidPersonException("A retired person cannot be merged.");
        }
        if (into == null && person.getContact() != null && people.existsBySheetIdAndContact(to, person.getContact())) {
            throw new InvalidPersonException("The contact is already in use on the other sheet.");
        }

        Rollup toBefore = Rollup.of(to, sheets, people, claims, rates, grades);
        if (target == null) {
            target = sheets.saveAndFlush(new Sheet(to));
        }
        claims.lockSheet(to);
        LocalDate today = target.getToday();

        // The person, with their grades over time and their aliases; or, merging, their name and aliases become the
        // other's aliases.
        List<PersonGrade> theirGrades = grades.findByPersonIdOrderByFromDateAsc(from);
        Person arrived;
        if (into != null) {
            arrived = into;
            Long intoId = into.getId();
            aliases.save(new PersonAlias(to, intoId, person.getName()));
            theirAliases.forEach(a -> aliases.save(new PersonAlias(to, intoId, a.getAlias())));
        } else {
            arrived = new Person(to, person.getName(), person.getGrade());
            arrived.setAddedOn(today);
            arrived.setTerms(person.getTerms());
            for (PersonTerms t : person.getDatedTerms()) {
                arrived.getDatedTerms().add(new PersonTerms(t.getFromDate(), t.getTerms()));
            }
            arrived.setContact(person.getContact());
            arrived.setTags(person.getTags());
            arrived.setOriginSheet(sheetId);
            arrived.setArrivedOn(today);
            arrived = people.saveAndFlush(arrived);
            for (PersonGrade grade : theirGrades) {
                PersonGrade copy = new PersonGrade(to, arrived.getId(), grade.getGrade(), grade.getFromDate());
                copy.setMadeOn(today);
                grades.save(copy);
            }
            for (PersonAlias alias : theirAliases) {
                aliases.save(new PersonAlias(to, arrived.getId(), alias.getAlias()));
            }
        }

        // Claims: copied in the order they are listed, so they keep it (after the other's, merging), each with its log
        // and comments.
        Long arrivedId = arrived.getId();
        List<Claim> theirs = new ArrayList<>(claims.findBySheetIdAndPersonIdOrderByIdDesc(sheetId, from));
        theirs.sort(Comparator.comparing(Claim::getListed).thenComparing(Claim::getId));
        Map<Claim, Claim> copies = new LinkedHashMap<>();
        Map<Long, LocalDate> openedOn = new HashMap<>();
        for (Claim claim : theirs) {
            Claim moved = claim.transferTo(to, arrivedId);
            moved.bind(claim.isStated(), claim.getStatedDue());
            Claim copy = claims.saveAndFlush(moved);
            copies.put(claim, copy);
            List<ClaimLog> lines = logs.findByClaimIdOrderByIdAsc(claim.getId());
            lines.stream().filter(l -> ClaimStatus.OPEN.name().equals(l.getStatus())).map(ClaimLog::getOnDate)
                    .findFirst().ifPresent(d -> openedOn.put(claim.getId(), d));
            lines.forEach(l -> logs.save(l.copyFor(copy)));
            logs.deleteAll(lines);
            List<ClaimComment> notes = comments.findByClaimIdOrderByIdAsc(claim.getId());
            notes.forEach(c -> comments.save(c.copyFor(copy)));
            comments.deleteAll(notes);
            List<ClaimLine> items = claimLines.findByClaimIdOrderByIdAsc(claim.getId());
            items.forEach(l -> claimLines.save(l.copyFor(copy)));
            claimLines.deleteAll(items);
        }

        // Numbers: the other sheet's next, in the order the claims were opened.
        List<Claim> numbered = new ArrayList<>(theirs.stream().filter(c -> c.getNumber() != null).toList());
        numbered.sort(Comparator.comparing((Claim c) -> openedOn.getOrDefault(c.getId(), LocalDate.MAX))
                .thenComparing(Claim::getId));
        Map<String, String> renumbered = new HashMap<>();
        boolean perPeriod = target.periodNumberingOn();
        List<Period> targetPeriods = periods.findBySheetIdOrderByFromDateAscIdAsc(to);
        for (Claim claim : numbered) {
            Claim copy = copies.get(claim);
            String number;
            if (perPeriod) {
                String prefix = targetPeriods.stream().filter(p -> p.contains(copy.getDueBy())).map(Period::getName)
                        .findFirst().orElse(null);
                String seq = prefix == null ? "X" : "P:" + prefix;
                number = (prefix == null ? "X" : prefix) + "-" + claims.takeNextInSequence(to, seq);
            } else {
                number = claims.takeNextInFiscalYear(to, target.getFiscalStartMonth(), copy.getDueBy());
            }
            copy.setNumber(number);
            renumbered.put(claim.getNumber(), number);
        }
        copies.forEach((claim, copy) -> {
            if (claim.getCorrects() != null) {
                copy.setCorrects(renumbered.get(claim.getCorrects()));
            }
            if (claim.getSplitFrom() != null) {
                copy.setSplitFrom(renumbered.get(claim.getSplitFrom()));
            }
            claims.save(copy);
        });
        claims.flush();

        // Statements go with them, before the other's own when merging; the claims bound there now are recorded as
        // their binding there.
        statements.moveAll(from, to, arrivedId);
        BindingLogs.record(bindings, claims, sheets, to, arrivedId);

        // They leave this sheet entirely; the tags their claims carried are still listed as used on it.
        Sheet here = sheets.findById(sheetId).orElse(null);
        if (here != null) {
            theirs.forEach(c -> here.addDepartedTags(c.getTagsUsed()));
            sheets.save(here);
        }
        // Plans go with them, each keeping the claims it made (now their copies), its pauses and its amount changes,
        // and the sheet it generates on while that is still a descendant of the sheet they arrive on.
        java.util.Set<String> below = sheets.descendantsOf(to);
        Map<Long, Long> copyIds = new HashMap<>();
        copies.forEach((claim, copy) -> copyIds.put(claim.getId(), copy.getId()));
        for (net.officefloor.hq.app.plan.Plan plan : plans.findBySheetIdOrderByIdAsc(sheetId)) {
            if (!from.equals(plan.getPersonId())) {
                continue;
            }
            net.officefloor.hq.app.plan.Plan moved = plans.saveAndFlush(new net.officefloor.hq.app.plan.Plan(to,
                    arrivedId, plan.getAmount(), plan.getStart(), plan.getEvery(), plan.getDay(), plan.getEnd(),
                    below.contains(plan.getTargetSheetId()) ? plan.getTargetSheetId() : null));
            List<net.officefloor.hq.app.plan.PlanClaim> made = planClaims.findByPlanId(plan.getId());
            for (net.officefloor.hq.app.plan.PlanClaim m : made) {
                Long copyId = copyIds.get(m.getClaimId());
                if (copyId != null) {
                    planClaims.save(new net.officefloor.hq.app.plan.PlanClaim(moved.getId(), m.getOnDate(), copyId));
                }
            }
            planClaims.deleteAll(made);
            planClaims.flush();
            List<net.officefloor.hq.app.plan.PlanPause> paused = planPauses.findByPlanIdOrderByIdAsc(plan.getId());
            for (net.officefloor.hq.app.plan.PlanPause p : paused) {
                planPauses.save(new net.officefloor.hq.app.plan.PlanPause(moved.getId(), p.getPausedOn(),
                        p.getResumedOn()));
            }
            planPauses.deleteAll(paused);
            planPauses.flush();
            List<net.officefloor.hq.app.plan.PlanChange> changed = planChanges.findByPlanIdOrderByIdAsc(plan.getId());
            for (net.officefloor.hq.app.plan.PlanChange c : changed) {
                planChanges.save(new net.officefloor.hq.app.plan.PlanChange(moved.getId(), c.getChangedOn(),
                        c.getAmount()));
            }
            planChanges.deleteAll(changed);
            planChanges.flush();
            plans.delete(plan);
            plans.flush();
        }
        claims.deleteAll(theirs);
        claims.flush();
        patrons.deleteAll(patrons.findByPersonIdOrderByIdAsc(from));
        patrons.deleteAll(patrons.findByPatronIdOrderByIdAsc(from));
        members.deleteAll(members.findByPersonIdOrderByIdAsc(from));
        crewMembers.deleteAll(crewMembers.findByPersonIdOrderByIdAsc(from));
        for (Crew crew : crews.findByLeadIdOrderByIdAsc(from)) {
            crew.setLeadId(null);
            crews.save(crew);
        }
        crews.flush();
        watches.deleteAll(watches.findByPersonIdOrderByIdAsc(from));
        grades.deleteAll(theirGrades);
        aliases.deleteAll(theirAliases);
        aliases.flush();
        people.delete(person);
        people.flush();
        if (into != null) {
            entityManager.refresh(into);
        }
        transferred.set(new PersonTransferred(person.getName(), sheetId, to, toBefore,
                into == null ? null : into.getName()));
    }
}
