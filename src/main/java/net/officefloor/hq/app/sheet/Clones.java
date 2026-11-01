package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimNumbers;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

/** Cloning a sheet to another id that was never used. */
@Service
public class Clones {

    private final JdbcTemplate jdbc;
    private final People people;
    private final Claims claims;
    private final ClaimNumbers numbers;

    public Clones(JdbcTemplate jdbc, People people, Claims claims, ClaimNumbers numbers) {
        this.jdbc = jdbc;
        this.people = people;
        this.claims = claims;
        this.numbers = numbers;
    }

    /** Copy sheet `from` to the never used sheet `to`, which keeps `from` as its source: its today, settings (dated ones, grade terms and
     *  permissions among them), roles, rates, holidays, void reasons and templates; its people (in list
     *  order) with their grades, terms, contact, aliases, holds, retirements and patrons; its pools and
     *  their members; its crews with their leads and members; its periods, all open; and its claims (listed in the order they were) with their
     *  status, dates, notes, tags, frozen figures, logs and comments. Each claim that had a number takes
     *  the clone's next one, in the order the claims were opened (by the sheet's today when each was
     *  opened, then by the order they were added to the clone). Statements are not copied: the statement
     *  count starts at 0 and no figure is bound; locks are not carried. Refused: a sheet already used. */
    public void copy(String from, String to) throws InvalidSheetRequestException {
        try {
            jdbc.update("INSERT INTO sheet (id, today) SELECT ?, today FROM sheet WHERE id = ?", to, from);
        } catch (DuplicateKeyException e) {
            throw new InvalidSheetRequestException("The other sheet has already been used");
        }

        jdbc.update("INSERT INTO sheet_setting (sheet_id, clerk_limit, floor, terms, late_pct, policy_date, "
                + "period_numbering_from, cap, grace_days, entry_date, pot, parent_id, fiscal_start_month, escalate_after, proof_kinds) SELECT ?, clerk_limit, "
                + "floor, terms, late_pct, policy_date, period_numbering_from, cap, grace_days, entry_date, pot, parent_id, "
                + "fiscal_start_month, escalate_after, proof_kinds "
                + "FROM sheet_setting WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO PUBLIC.sheet_source (sheet_id, source_id) VALUES (?, ?)", to, from);
        jdbc.update("INSERT INTO sheet_setting_dated (sheet_id, setting_key, from_date, setting_value) "
                + "SELECT ?, setting_key, from_date, setting_value FROM sheet_setting_dated WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_grade_terms (sheet_id, grade, terms) "
                + "SELECT ?, grade, terms FROM sheet_grade_terms WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_permission (sheet_id, action_key, roles) "
                + "SELECT ?, action_key, roles FROM sheet_permission WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_role (sheet_id, user_name, role) "
                + "SELECT ?, user_name, role FROM sheet_role WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version, forward_after) "
                + "SELECT ?, grade, pct, from_date, to_date, version, forward_after FROM rate WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date, forward_after) "
                + "SELECT ?, grade, from_date, version, pct, to_date, forward_after FROM rate_version WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_holiday (sheet_id, holiday_date) "
                + "SELECT ?, holiday_date FROM sheet_holiday WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO sheet_void_reason (sheet_id, reason, position) "
                + "SELECT ?, reason, position FROM sheet_void_reason WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note) "
                + "SELECT ?, name, amount, note FROM claim_template WHERE sheet_id = ?", to, from);
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, closed, closed_total, soft, cap) "
                + "SELECT ?, name, from_date, to_date, FALSE, NULL, FALSE, cap FROM period WHERE sheet_id = ?", to, from);

        // People, in list order.
        List<Person> all = people.onSheet(from);
        Map<Long, Long> personIds = new HashMap<>();
        for (Person person : all) personIds.put(person.getId(), insertPerson(person.getId(), to));
        for (Person person : all) {
            long id = personIds.get(person.getId());
            jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) "
                    + "SELECT ?, ?, grade, from_date FROM person_grade WHERE person_id = ? ORDER BY id",
                    to, id, person.getId());
            jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, alias_key) "
                    + "SELECT ?, ?, alias, alias_key FROM person_alias WHERE person_id = ? ORDER BY id",
                    to, id, person.getId());
            jdbc.update("INSERT INTO person_hold (sheet_id, person_id, from_date, to_date) "
                    + "SELECT ?, ?, from_date, to_date FROM person_hold WHERE person_id = ? ORDER BY id",
                    to, id, person.getId());
        }
        for (Map<String, Object> patron : jdbc.queryForList(
                "SELECT person_id, patron_id FROM person_patron WHERE sheet_id = ? ORDER BY id", from)) {
            jdbc.update("INSERT INTO person_patron (sheet_id, person_id, patron_id) VALUES (?, ?, ?)", to,
                    personIds.get(((Number) patron.get("PERSON_ID")).longValue()),
                    personIds.get(((Number) patron.get("PATRON_ID")).longValue()));
        }

        jdbc.update("INSERT INTO pool (sheet_id, name, position) SELECT ?, name, position FROM pool WHERE sheet_id = ?",
                to, from);
        for (Map<String, Object> member : jdbc.queryForList("SELECT pool, person_id, member_pool, share, position, "
                + "from_date, to_date FROM pool_member WHERE sheet_id = ? ORDER BY position", from)) {
            Number personId = (Number) member.get("PERSON_ID");
            jdbc.update("INSERT INTO pool_member (sheet_id, pool, person_id, member_pool, share, position, from_date, "
                    + "to_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", to, member.get("POOL"),
                    personId == null ? null : personIds.get(personId.longValue()), member.get("MEMBER_POOL"),
                    member.get("SHARE"), member.get("POSITION"), member.get("FROM_DATE"), member.get("TO_DATE"));
        }
        for (Map<String, Object> crew : jdbc.queryForList("SELECT name, lead_id, position FROM crew "
                + "WHERE sheet_id = ? ORDER BY position", from)) {
            Long leadId = personIds.get(((Number) crew.get("LEAD_ID")).longValue());
            if (leadId == null) continue;
            jdbc.update("INSERT INTO crew (sheet_id, name, lead_id, position) VALUES (?, ?, ?, ?)", to,
                    crew.get("NAME"), leadId, crew.get("POSITION"));
        }
        for (Map<String, Object> member : jdbc.queryForList("SELECT m.crew, m.person_id, m.position FROM crew_member m "
                + "JOIN crew c ON c.sheet_id = ? AND c.name = m.crew WHERE m.sheet_id = ? ORDER BY m.position", to, from)) {
            Long personId = personIds.get(((Number) member.get("PERSON_ID")).longValue());
            if (personId == null) continue;
            jdbc.update("INSERT INTO crew_member (sheet_id, crew, person_id, position) VALUES (?, ?, ?, ?)", to,
                    member.get("CREW"), personId, member.get("POSITION"));
        }

        // Claims: person by person in list order, each person's oldest listed first, so they keep their order.
        List<Claim> kept = new ArrayList<>();
        for (Person person : all) {
            List<Claim> theirs = new ArrayList<>(claims.ofPerson(from, person.getId()));
            Collections.reverse(theirs);
            kept.addAll(theirs);
        }
        Map<Long, Long> claimIds = new HashMap<>();
        for (Claim claim : kept) {
            long newId = insertClaim(claim.getId(), to, personIds.get(claim.getPersonId()));
            claimIds.put(claim.getId(), newId);
            jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, on_date, due) "
                    + "SELECT ?, ?, status, by_user, on_date, due FROM claim_log WHERE claim_id = ? ORDER BY id",
                    to, newId, claim.getId());
            jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, on_date, comment_text) "
                    + "SELECT ?, ?, by_user, on_date, comment_text FROM claim_comment WHERE claim_id = ? ORDER BY id",
                    to, newId, claim.getId());
            jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) "
                    + "SELECT ?, ?, label, qty, unit FROM claim_line WHERE claim_id = ? ORDER BY id",
                    to, newId, claim.getId());
        }
        Map<String, String> renumbered = renumber(kept, claimIds, to);
        for (Claim claim : kept) {
            long newId = claimIds.get(claim.getId());
            jdbc.update("UPDATE claim SET number = ?, corrects = ?, split_from = ? WHERE id = ?",
                    renumbered.get(claim.getNumber()), mapped(renumbered, claim.getCorrects()),
                    mapped(renumbered, claim.getSplitFrom()), newId);
        }
    }

    private static String mapped(Map<String, String> renumbered, String number) {
        return number == null ? null : renumbered.getOrDefault(number, number);
    }

    private long insertPerson(long personId, String to) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact, tags, retired_from, past_retirements, dated_terms, "
                            + "position, origin_sheet, origin_name) SELECT ?, name, grade, terms, contact, tags, retired_from, "
                            + "past_retirements, dated_terms, position, origin_sheet, origin_name FROM person WHERE id = ?",
                    new String[] { "id" });
            ps.setString(1, to);
            ps.setLong(2, personId);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    private long insertClaim(long claimId, String to, long personId) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("INSERT INTO claim (sheet_id, person_id, amount, claim_date, "
                    + "note, status, number, due, opened_by, stated, stated_due, corrects, due_by, deferred_until, "
                    + "proofs, proof_photos, split_from, locked_by, late_entry, reminded_on, escalated, waived, held, ref, tags, tags_used, void_reason, "
                    + "edited_by, due_terms, batch, added_seq) SELECT ?, ?, amount, claim_date, note, status, NULL, due, opened_by, "
                    + "FALSE, NULL, NULL, due_by, deferred_until, proofs, proof_photos, NULL, NULL, late_entry, reminded_on, escalated, waived, held, "
                    + "ref, tags, tags_used, void_reason, edited_by, due_terms, batch, COALESCE(added_seq, id) FROM claim WHERE id = ?",
                    new String[] { "id" });
            ps.setString(1, to);
            ps.setLong(2, personId);
            ps.setLong(3, claimId);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /** The clone's number of each claim number of this sheet, given in the order the claims were opened. */
    private Map<String, String> renumber(List<Claim> kept, Map<Long, Long> claimIds, String to) {
        Map<Long, LocalDate> openedOn = new HashMap<>();
        for (Claim claim : kept) {
            if (claim.getNumber() == null) continue;
            List<Date> dates = jdbc.queryForList(
                    "SELECT on_date FROM claim_log WHERE claim_id = ? AND status = 'OPEN' ORDER BY id",
                    Date.class, claim.getId());
            openedOn.put(claim.getId(), dates.isEmpty() ? claim.getDate() : dates.get(0).toLocalDate());
        }
        List<Claim> numbered = kept.stream().filter(c -> c.getNumber() != null)
                .sorted(Comparator.comparing((Claim c) -> openedOn.get(c.getId()))
                        .thenComparing(c -> claimIds.get(c.getId())))
                .toList();
        Map<String, String> renumbered = new HashMap<>();
        for (Claim claim : numbered) {
            Claim there = new Claim(null, to, 0, claim.getAmount(), claim.getDate(), claim.getNote(),
                    claim.getStatus(), null, claim.getDue(), claim.getOpenedBy());
            there.setDueBy(claim.getDueBy());
            renumbered.put(claim.getNumber(), numbers.next(there));
        }
        return renumbered;
    }
}
