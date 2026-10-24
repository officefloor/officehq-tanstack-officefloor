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
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClerkLimitException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.pool.PoolMemberRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Transfers the person to another sheet by id; only a steward may, not a retired person, not to this sheet itself,
 * nor to an archived sheet, nor when their contact is already on the other sheet. When their name or an alias of theirs
 * is already someone's name or alias there (ignoring case), they are merged into that person instead, exactly as a
 * merge does (not into a retired person): their claims are listed after that person's, each losing any figure a
 * statement bound it to, their name and aliases become that person's aliases, and their grades, terms and contact are
 * dropped. Otherwise they arrive there with every grade they were given
 * (and its from-date), their terms, contact and aliases, and their claims with their status, dates, notes, tags,
 * frozen dues, status logs and comments, listed in the same order. Each claim that had a number takes the other
 * sheet's next one, in the order they were opened (by the sheet's today when each was opened, then by the order they
 * were added). Figures a statement bound are lost (they have no statements there) and references are not carried.
 * They leave this sheet entirely: their claims, statements, holds, patron links (both ways), pool memberships and
 * watchers are dropped.
 */
public class ApplyTransfer {
    public void apply(@Val TransferInput input, @Val Person person, PersonRepository people, ClaimRepository claims,
            ClaimLogRepository logs, ClaimCommentRepository comments, PersonAliasRepository aliases,
            PatronRepository patrons, PoolMemberRepository members, StatementRepository statements,
            WatchRepository watches, PersonGradeRepository grades, RoleRepository roles, SheetRepository sheets,
            RateRepository rates, PeriodRepository periods, EntityManager entityManager,
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
            arrived.setContact(person.getContact());
            arrived.setOriginSheet(sheetId);
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
            Claim copy = claims.saveAndFlush(claim.transferTo(to, arrivedId));
            copies.put(claim, copy);
            List<ClaimLog> lines = logs.findByClaimIdOrderByIdAsc(claim.getId());
            lines.stream().filter(l -> ClaimStatus.OPEN.name().equals(l.getStatus())).map(ClaimLog::getOnDate)
                    .findFirst().ifPresent(d -> openedOn.put(claim.getId(), d));
            lines.forEach(l -> logs.save(l.copyFor(copy)));
            logs.deleteAll(lines);
            List<ClaimComment> notes = comments.findByClaimIdOrderByIdAsc(claim.getId());
            notes.forEach(c -> comments.save(c.copyFor(copy)));
            comments.deleteAll(notes);
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
                claims.takeNextNumber(to);
                number = String.valueOf(claims.lastNumber(to));
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

        // They leave this sheet entirely.
        claims.deleteAll(theirs);
        claims.flush();
        patrons.deleteAll(patrons.findByPersonIdOrderByIdAsc(from));
        patrons.deleteAll(patrons.findByPatronIdOrderByIdAsc(from));
        members.deleteAll(members.findByPersonIdOrderByIdAsc(from));
        watches.deleteAll(watches.findByPersonIdOrderByIdAsc(from));
        statements.deleteAll(statements.findByPersonIdOrderByIdAsc(from));
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
