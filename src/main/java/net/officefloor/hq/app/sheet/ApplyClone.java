package net.officefloor.hq.app.sheet;

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
import net.officefloor.hq.app.claim.ClaimLineRepository;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Clones the sheet to another id; only a steward may, and only to an id never used (no sheet is stored for it). The
 * clone gets this sheet's today, settings (with their log), roles, rates (with their log), holidays, void reasons,
 * templates, people (grades, terms, contacts, aliases, holds, retirements, patrons) in list order, pools with their
 * members, crews with their leads and members, periods (all open) and claims, each person's in the order listed, with their status, frozen figures,
 * dates, notes, tags, references, status logs, comments and lines. Each claim that had a number takes the clone's next one,
 * in the order they were opened (by the sheet's today when each was opened, then by the order they were added to the
 * clone). The clone remembers the sheet it was cloned from, whose rates it falls back to. Statements are not copied, so no figure is bound there; nor are watchers, delegations or the as-of history.
 */
public class ApplyClone {
    public void apply(@Val CloneInput input, @Val Sheet sheet, SheetRepository sheets, RoleRepository roles,
            PersonRepository people, ClaimRepository claims, ClaimLogRepository logs, ClaimCommentRepository comments,
            ClaimLineRepository claimLines,
            PeriodRepository periods, EntityManager entityManager,
            @RequestHeader(name = "X-User", required = false) String userHeader, Out<SheetCloned> cloned)
            throws InvalidCloneException {
        String from = sheet.getId();
        String user = AuditUser.decode(userHeader);
        if (!Role.STEWARD.equals(roles.roleOf(from, user))) {
            throw new InvalidCloneException("Only a steward can clone the sheet.");
        }
        String to = input.toSheet();
        if (sheets.existsById(to)) {
            throw new InvalidCloneException("The sheet " + to + " is already in use.");
        }
        claims.lockSheet(from);

        // The sheet, with its today and undated dates; then everything kept per sheet with no links of its own.
        Sheet clone = new Sheet(to);
        clone.setToday(sheet.getToday());
        clone.setPolicyDate(sheet.getPolicyDate());
        clone.setPeriodNumberingFrom(sheet.getPeriodNumberingFrom());
        clone.setEntryDate(sheet.getEntryDate());
        clone.setClonedFrom(from);
        clone = sheets.saveAndFlush(clone);
        copyRows(entityManager, from, to, "sheet_setting", "setting_key, from_date, setting_value");
        copyRows(entityManager, from, to, "setting_log", "setting_key, from_date, setting_value, made_on");
        copyRows(entityManager, from, to, "sheet_role", "user_name, role");
        copyRows(entityManager, from, to, "rate", "grade, pct, from_date, to_date, version");
        copyRows(entityManager, from, to, "rate_version", "grade, from_date, version, pct, to_date, replaced_on");
        copyRows(entityManager, from, to, "rate_log", "grade, pct, from_date, to_date, made_on, forward_after");
        copyRows(entityManager, from, to, "holiday", "holiday_date");
        copyRows(entityManager, from, to, "void_reason", "reason");
        copyRows(entityManager, from, to, "claim_template", "name, amount, note");
        entityManager.createNativeQuery("INSERT INTO period (sheet_id, name, from_date, to_date)"
                + " SELECT :to, name, from_date, to_date FROM period WHERE sheet_id = :from ORDER BY id")
                .setParameter("to", to).setParameter("from", from).executeUpdate();

        // People, in list order, each with their grades over time, holds, retirements and aliases.
        Map<Long, Long> personIds = new LinkedHashMap<>();
        for (Person person : people.findListed(from)) {
            entityManager.createNativeQuery("INSERT INTO person (sheet_id, name, grade, version, retired_from, terms,"
                    + " contact, tags, added_on, retired_made_on, origin_sheet) SELECT :to, name, grade, 0, retired_from, terms,"
                    + " contact, tags, added_on, retired_made_on, origin_sheet FROM person WHERE id = :id")
                    .setParameter("to", to).setParameter("id", person.getId()).executeUpdate();
            Long copy = ((Number) entityManager.createNativeQuery(
                    "SELECT id FROM person WHERE sheet_id = :to AND name_key = LOWER(:name)")
                    .setParameter("to", to).setParameter("name", person.getName()).getSingleResult()).longValue();
            personIds.put(person.getId(), copy);
            copyPersonRows(entityManager, person.getId(), copy, "person_hold", "from_date, to_date, made_on");
            copyPersonRows(entityManager, person.getId(), copy, "person_retirement",
                    "from_date, to_date, retire_made_on, made_on");
            entityManager.createNativeQuery("INSERT INTO person_grade (sheet_id, person_id, grade, from_date, made_on)"
                    + " SELECT :to, :copy, grade, from_date, made_on FROM person_grade WHERE person_id = :id ORDER BY id")
                    .setParameter("to", to).setParameter("copy", copy).setParameter("id", person.getId())
                    .executeUpdate();
            entityManager.createNativeQuery("INSERT INTO person_alias (sheet_id, person_id, alias)"
                    + " SELECT :to, :copy, alias FROM person_alias WHERE person_id = :id ORDER BY id")
                    .setParameter("to", to).setParameter("copy", copy).setParameter("id", person.getId())
                    .executeUpdate();
        }
        for (Object[] link : rows(entityManager, "SELECT person_id, patron_id FROM person_patron"
                + " WHERE sheet_id = :from ORDER BY id", from)) {
            Long person = personIds.get(((Number) link[0]).longValue());
            Long patron = personIds.get(((Number) link[1]).longValue());
            if (person != null && patron != null) {
                entityManager.createNativeQuery("INSERT INTO person_patron (sheet_id, person_id, patron_id)"
                        + " VALUES (:to, :person, :patron)").setParameter("to", to).setParameter("person", person)
                        .setParameter("patron", patron).executeUpdate();
            }
        }

        // Pools, then their members (a person or another pool of the sheet).
        Map<Long, Long> poolIds = new HashMap<>();
        for (Object[] pool : rows(entityManager, "SELECT id, name FROM pool WHERE sheet_id = :from ORDER BY id", from)) {
            entityManager.createNativeQuery("INSERT INTO pool (sheet_id, name) VALUES (:to, :name)")
                    .setParameter("to", to).setParameter("name", pool[1]).executeUpdate();
            Long copy = ((Number) entityManager.createNativeQuery(
                    "SELECT id FROM pool WHERE sheet_id = :to AND name = :name")
                    .setParameter("to", to).setParameter("name", pool[1]).getSingleResult()).longValue();
            poolIds.put(((Number) pool[0]).longValue(), copy);
        }
        for (Object[] member : rows(entityManager, "SELECT m.id, m.pool_id, m.person_id, m.member_pool_id"
                + " FROM pool_member m JOIN pool p ON p.id = m.pool_id WHERE p.sheet_id = :from ORDER BY m.id", from)) {
            Long pool = poolIds.get(((Number) member[1]).longValue());
            Long person = member[2] == null ? null : personIds.get(((Number) member[2]).longValue());
            Long memberPool = member[3] == null ? null : poolIds.get(((Number) member[3]).longValue());
            if (pool == null || (person == null && memberPool == null)) {
                continue;
            }
            String column = person != null ? "person_id" : "member_pool_id";
            entityManager.createNativeQuery("INSERT INTO pool_member (pool_id, " + column + ", share, from_date, to_date)"
                    + " SELECT :pool, :member, share, from_date, to_date FROM pool_member WHERE id = :id")
                    .setParameter("pool", pool).setParameter("member", person != null ? person : memberPool)
                    .setParameter("id", ((Number) member[0]).longValue()).executeUpdate();
        }

        // Crews, with their leads, then their members.
        Map<Long, Long> crewIds = new HashMap<>();
        for (Object[] crew : rows(entityManager, "SELECT id, name, lead_id FROM crew WHERE sheet_id = :from ORDER BY id", from)) {
            if (crew[2] == null) {
                // A crew with no lead (transferred away) is copied with none.
                entityManager.createNativeQuery("INSERT INTO crew (sheet_id, name) VALUES (:to, :name)")
                        .setParameter("to", to).setParameter("name", crew[1]).executeUpdate();
            } else {
                Long lead = personIds.get(((Number) crew[2]).longValue());
                if (lead == null) {
                    continue;
                }
                entityManager.createNativeQuery("INSERT INTO crew (sheet_id, name, lead_id) VALUES (:to, :name, :lead)")
                        .setParameter("to", to).setParameter("name", crew[1]).setParameter("lead", lead).executeUpdate();
            }
            Long copy = ((Number) entityManager.createNativeQuery(
                    "SELECT id FROM crew WHERE sheet_id = :to AND name = :name")
                    .setParameter("to", to).setParameter("name", crew[1]).getSingleResult()).longValue();
            crewIds.put(((Number) crew[0]).longValue(), copy);
        }
        for (Object[] member : rows(entityManager, "SELECT m.crew_id, m.person_id"
                + " FROM crew_member m JOIN crew c ON c.id = m.crew_id WHERE c.sheet_id = :from ORDER BY m.id", from)) {
            Long crew = crewIds.get(((Number) member[0]).longValue());
            Long person = personIds.get(((Number) member[1]).longValue());
            if (crew != null && person != null) {
                entityManager.createNativeQuery("INSERT INTO crew_member (crew_id, person_id) VALUES (:crew, :person)")
                        .setParameter("crew", crew).setParameter("person", person).executeUpdate();
            }
        }

        // Claims: each person's in the order they are listed, so they keep it, each with its log and comments.
        List<Claim> all = claims.findBySheetIdOrderByIdDesc(from);
        Map<Claim, Claim> copies = new LinkedHashMap<>();
        Map<Claim, LocalDate> openedOn = new HashMap<>();
        for (Map.Entry<Long, Long> person : personIds.entrySet()) {
            List<Claim> theirs = new ArrayList<>(all.stream().filter(c -> person.getKey().equals(c.getPersonId()))
                    .toList());
            theirs.sort(Comparator.comparing(Claim::getListed).thenComparing(Claim::getId));
            for (Claim claim : theirs) {
                Claim made = claim.transferTo(to, person.getValue());
                made.setRef(claim.getRef());
                Claim copy = claims.saveAndFlush(made);
                copies.put(claim, copy);
                List<ClaimLog> lines = logs.findByClaimIdOrderByIdAsc(claim.getId());
                lines.stream().filter(l -> ClaimStatus.OPEN.name().equals(l.getStatus())).map(ClaimLog::getOnDate)
                        .findFirst().ifPresent(d -> openedOn.put(claim, d));
                lines.forEach(l -> logs.save(l.copyFor(copy)));
                comments.findByClaimIdOrderByIdAsc(claim.getId()).forEach(c -> comments.save(c.copyFor(copy)));
                claimLines.findByClaimIdOrderByIdAsc(claim.getId()).forEach(l -> claimLines.save(l.copyFor(copy)));
            }
        }

        // Numbers: the clone's own, in the order the claims were opened.
        List<Claim> numbered = new ArrayList<>(copies.keySet().stream().filter(c -> c.getNumber() != null).toList());
        numbered.sort(Comparator.comparing((Claim c) -> openedOn.getOrDefault(c, LocalDate.MAX))
                .thenComparing(c -> copies.get(c).getId()));
        Map<String, String> renumbered = new HashMap<>();
        boolean perPeriod = clone.periodNumberingOn();
        List<Period> clonePeriods = periods.findBySheetIdOrderByFromDateAscIdAsc(to);
        for (Claim claim : numbered) {
            Claim copy = copies.get(claim);
            String number;
            if (perPeriod) {
                String prefix = clonePeriods.stream().filter(p -> p.contains(copy.getDueBy())).map(Period::getName)
                        .findFirst().orElse(null);
                String seq = prefix == null ? "X" : "P:" + prefix;
                number = (prefix == null ? "X" : prefix) + "-" + claims.takeNextInSequence(to, seq);
            } else {
                number = claims.takeNextInFiscalYear(to, clone.getFiscalStartMonth(), copy.getDueBy());
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
        // What was written past the persistence context is read afresh from here on.
        entityManager.clear();
        cloned.set(new SheetCloned(from, to));
    }

    /** Copies a sheet's rows of a table with no links of their own, in the order they were made. */
    private static void copyRows(EntityManager entityManager, String from, String to, String table, String columns) {
        entityManager.createNativeQuery("INSERT INTO " + table + " (sheet_id, " + columns + ") SELECT :to, " + columns
                + " FROM " + table + " WHERE sheet_id = :from ORDER BY id")
                .setParameter("to", to).setParameter("from", from).executeUpdate();
    }

    /** Copies a person's rows of a table keyed by the person alone. */
    private static void copyPersonRows(EntityManager entityManager, Long from, Long to, String table, String columns) {
        entityManager.createNativeQuery("INSERT INTO " + table + " (person_id, " + columns + ") SELECT :to, " + columns
                + " FROM " + table + " WHERE person_id = :from")
                .setParameter("to", to).setParameter("from", from).executeUpdate();
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(EntityManager entityManager, String sql, String from) {
        return entityManager.createNativeQuery(sql).setParameter("from", from).getResultList();
    }
}
