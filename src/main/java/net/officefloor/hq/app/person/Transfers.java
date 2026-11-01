package net.officefloor.hq.app.person;

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
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

/** Moving a person from their sheet to another. */
@Service
public class Transfers {

    private final JdbcTemplate jdbc;
    private final Claims claims;
    private final ClaimNumbers numbers;
    private final People people;
    private final Sheets sheets;

    public Transfers(JdbcTemplate jdbc, Claims claims, ClaimNumbers numbers, People people, Sheets sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.claims = claims;
        this.numbers = numbers;
        this.people = people;
    }

    /** Transfer the person to sheet `to`, answering their id there. They arrive with every grade they
     *  were given (and its from-date), their terms (dated ones too), contact and aliases, and their claims (listed in
     *  the order they were) with their status, dates, notes, tags, frozen figures, logs and comments;
     *  each claim that had a number takes the other sheet's next one, in the order the claims were
     *  opened (by the sheet's today when each was opened, then by the order they were added). Each claim
     *  keeps the figure a statement bound it to, and the person arrives with their statements (count,
     *  last one and history); references, locks and settle batches are not carried. The person then leaves their sheet entirely: with their claims go
     *  their statements, holds, watches, patron links (either way), pool and crew memberships and period totals.
     *  When `into` (a person on sheet `to`) is given, the person is merged into them instead, as a merge
     *  does: their claims arrive as into's, listed after into's own (each renumbered as above, keeping its
     *  bound figure); into's statement count becomes their own plus the person's (see
     *  {@link People#takeStatements});
     *  their name and aliases become into's aliases; their grades, terms and contact are lost; and the
     *  answer is into's id. */
    public long transfer(Person person, String to, Person into) {
        // The person arrives knowing the sheet they came from and their name there (the audit follows them back).
        long fromId = person.getId();
        long toId = into != null ? mergeInto(person, into) : insertPerson(person, to);
        Person receiving = into != null ? into : people.find(to, toId);
        people.takeStatements(person, receiving);

        List<Claim> kept = new ArrayList<>(claims.ofPerson(person.getSheetId(), fromId));
        Collections.reverse(kept); // oldest listed first, so they keep their order
        Map<Long, String> renumbered = renumber(kept, to);
        for (Claim claim : kept) {
            long newId = insertClaim(claim, to, toId, renumbered.get(claim.getId()));
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

        for (Claim claim : kept) {
            jdbc.update("DELETE FROM claim_log WHERE claim_id = ?", claim.getId());
            jdbc.update("DELETE FROM claim_comment WHERE claim_id = ?", claim.getId());
            jdbc.update("DELETE FROM claim_line WHERE claim_id = ?", claim.getId());
            jdbc.update("DELETE FROM claim WHERE id = ?", claim.getId());
        }
        jdbc.update("DELETE FROM person_patron WHERE person_id = ? OR patron_id = ?", fromId, fromId);
        jdbc.update("DELETE FROM pool_member WHERE person_id = ?", fromId);
        jdbc.update("DELETE FROM crew_member WHERE person_id = ?", fromId);
        jdbc.update("DELETE FROM plan_claim WHERE plan_id IN (SELECT id FROM plan WHERE person_id = ?)", fromId);
        jdbc.update("DELETE FROM plan_pause WHERE plan_id IN (SELECT id FROM plan WHERE person_id = ?)", fromId);
        jdbc.update("DELETE FROM plan WHERE person_id = ?", fromId);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND person_id = ?", person.getSheetId(), fromId);
        jdbc.update("DELETE FROM period_opening WHERE sheet_id = ? AND person_id = ?", person.getSheetId(), fromId);
        for (String table : List.of("person_alias", "person_statement", "person_hold", "person_watch", "person_grade", "person_snapshot")) {
            jdbc.update("DELETE FROM " + table + " WHERE person_id = ?", fromId);
        }
        jdbc.update("DELETE FROM person WHERE id = ?", fromId);
        return toId;
    }

    /** The sheet's today when the person arrived here by a transfer; null when they did not. */
    public LocalDate arrivedOn(long personId) {
        List<Date> dates = jdbc.queryForList("SELECT origin_date FROM person WHERE id = ?", Date.class, personId);
        return dates.isEmpty() || dates.get(0) == null ? null : dates.get(0).toLocalDate();
    }

    /** The person's name and aliases become into's aliases; answers into's id. */
    private long mergeInto(Person person, Person into) {
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, alias_key) "
                + "SELECT ?, ?, alias, alias_key FROM person_alias WHERE person_id = ? ORDER BY id",
                into.getSheetId(), into.getId(), person.getId());
        String name = person.getName().trim();
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, alias_key) VALUES (?, ?, ?, LOWER(?))",
                into.getSheetId(), into.getId(), name, name);
        return into.getId();
    }

    private long insertPerson(Person person, String to) {
        Date arrived = Date.valueOf(sheets.today(to));
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO person (sheet_id, name, grade, terms, contact, tags, origin_sheet, origin_name, dated_terms, origin_date, position) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                            + People.END_POSITION + ")", new String[] { "id" });
            ps.setString(1, to);
            ps.setString(2, person.getName());
            ps.setString(3, person.getFirstGrade());
            ps.setObject(4, person.getTerms(), java.sql.Types.INTEGER);
            ps.setString(5, person.getContact());
            ps.setString(6, person.getTags());
            ps.setString(7, person.getSheetId());
            ps.setString(8, person.getName());
            ps.setString(9, People.datedTerms(person.getDatedTerms()));
            ps.setDate(10, arrived);
            ps.setString(11, to);
            return ps;
        }, keys);
        long toId = keys.getKey().longValue();
        jdbc.update("INSERT INTO person_grade (sheet_id, person_id, grade, from_date) "
                + "SELECT ?, ?, grade, from_date FROM person_grade WHERE person_id = ? ORDER BY id",
                to, toId, person.getId());
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, alias_key) "
                + "SELECT ?, ?, alias, alias_key FROM person_alias WHERE person_id = ? ORDER BY id",
                to, toId, person.getId());
        return toId;
    }

    /** The new number on sheet `to` of each claim that had one, by its id, given in the order opened. */
    private Map<Long, String> renumber(List<Claim> kept, String to) {
        Map<Long, LocalDate> openedOn = new HashMap<>();
        for (Claim claim : kept) {
            if (claim.getNumber() == null) continue;
            List<Date> dates = jdbc.queryForList(
                    "SELECT on_date FROM claim_log WHERE claim_id = ? AND status = 'OPEN' ORDER BY id",
                    Date.class, claim.getId());
            openedOn.put(claim.getId(), dates.isEmpty() ? claim.getDate() : dates.get(0).toLocalDate());
        }
        List<Claim> numbered = kept.stream().filter(c -> c.getNumber() != null)
                .sorted(Comparator.comparing((Claim c) -> openedOn.get(c.getId())).thenComparing(Claim::getId))
                .toList();
        Map<Long, String> renumbered = new HashMap<>();
        for (Claim claim : numbered) {
            Claim there = new Claim(null, to, 0, claim.getAmount(), claim.getDate(), claim.getNote(),
                    claim.getStatus(), null, claim.getDue(), claim.getOpenedBy());
            there.setDueBy(claim.getDueBy());
            renumbered.put(claim.getId(), numbers.next(there));
        }
        return renumbered;
    }

    private long insertClaim(Claim claim, String to, long toId, String number) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("INSERT INTO claim (sheet_id, person_id, amount, claim_date, "
                    + "note, status, number, due, opened_by, stated, stated_due, corrects, due_by, deferred_until, "
                    + "proofs, proof_photos, split_from, locked_by, late_entry, reminded_on, escalated, waived, held, ref, tags, tags_used, void_reason, "
                    + "edited_by, due_terms, batch) SELECT ?, ?, amount, claim_date, note, status, ?, due, opened_by, "
                    + "stated, stated_due, corrects, due_by, deferred_until, proofs, proof_photos, split_from, NULL, late_entry, reminded_on, escalated, waived, held, "
                    + "NULL, tags, tags_used, void_reason, edited_by, due_terms, NULL FROM claim WHERE id = ?",
                    new String[] { "id" });
            ps.setString(1, to);
            ps.setLong(2, toId);
            ps.setString(3, number);
            ps.setLong(4, claim.getId());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }
}
