package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, sheet_id, person_id, amount, claim_date, note, status, claim_number, due, opened_by, stated, stated_due, corrects, due_by, deferred_until, proofs, split_from";

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> findAll(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY listed DESC, id DESC",
                ClaimRepository::map, sheetId);
    }

    /** The claim with this id on this sheet, or null. */
    public Claim find(String sheetId, long id) {
        List<Claim> found = jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?",
                ClaimRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Insert a new claim (null id) or update an existing one; the claim as stored. */
    public Claim save(Claim claim) {
        if (claim.id() != null) {
            jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, status = ?, claim_number = ?, due = ?, opened_by = ?, corrects = ? WHERE id = ? AND sheet_id = ?",
                    claim.amount(), Date.valueOf(claim.date()), claim.note(), claim.status().name(), claim.number(),
                    claim.due(), claim.openedBy(), claim.corrects(), claim.id(), claim.sheetId());
            return claim;
        }
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, status, corrects, due_by, claim_number, opened_by, split_from) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, claim.sheetId());
            ps.setLong(2, claim.personId());
            ps.setBigDecimal(3, claim.amount());
            ps.setDate(4, Date.valueOf(claim.date()));
            ps.setString(5, claim.note());
            ps.setString(6, claim.status().name());
            ps.setString(7, claim.corrects());
            ps.setDate(8, Date.valueOf(claim.dueBy()));
            ps.setString(9, claim.number());
            ps.setString(10, claim.openedBy());
            ps.setString(11, claim.splitFrom());
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), claim.status(), claim.number(), claim.due(), claim.openedBy(), false, null,
                claim.corrects(), claim.dueBy(), null, 0, claim.splitFrom());
    }

    /** The person's claims, newest first. */
    public List<Claim> findForPerson(String sheetId, long personId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed DESC, id DESC",
                ClaimRepository::map, sheetId, personId);
    }

    /**
     * Move the claim to another person: it loses any figure a statement bound it to (live again) and is listed as
     * their newest claim; the claim as stored.
     */
    public Claim reassign(Claim claim, long personId) {
        jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL,"
                + " listed = NEXT VALUE FOR claim_listed_seq WHERE id = ? AND sheet_id = ?",
                personId, claim.id(), claim.sheetId());
        return find(claim.sheetId(), claim.id());
    }

    /** Bind the claim to a statement's figure (null: n/a), or unbind it (live again) when not stated. */
    public void bindStatement(Claim claim, boolean stated, BigDecimal statedDue) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE id = ? AND sheet_id = ?",
                stated, stated ? statedDue : null, claim.id(), claim.sheetId());
    }

    /** Unbind (live again) each of the person's claims bound by their last statement. */
    public void unbindStatement(String sheetId, long personId) {
        jdbc.update("UPDATE claim SET stated = FALSE, stated_due = NULL WHERE sheet_id = ? AND person_id = ? AND stated",
                sheetId, personId);
    }

    /** Defer the claim until its {@code deferredUntil}. */
    public void defer(Claim claim) {
        jdbc.update("UPDATE claim SET deferred_until = ? WHERE id = ? AND sheet_id = ?",
                Date.valueOf(claim.deferredUntil()), claim.id(), claim.sheetId());
    }

    /** Store the claim's {@code date} and {@code number}. */
    public void redate(Claim claim) {
        jdbc.update("UPDATE claim SET claim_date = ?, claim_number = ? WHERE id = ? AND sheet_id = ?",
                Date.valueOf(claim.date()), claim.number(), claim.id(), claim.sheetId());
    }

    /** Store the claim's {@code proofs} count. */
    public void saveProofs(Claim claim) {
        jdbc.update("UPDATE claim SET proofs = ? WHERE id = ? AND sheet_id = ?", claim.proofs(), claim.id(),
                claim.sheetId());
    }

    /** Mark the claim a late entry: added while the period containing its date was soft-closed. */
    public void markLateEntry(Claim claim) {
        jdbc.update("UPDATE claim SET late_entry = TRUE WHERE id = ? AND sheet_id = ?", claim.id(), claim.sheetId());
    }

    /** Whether the claim is a late entry. */
    public boolean lateEntry(String sheetId, long id) {
        return !jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND id = ? AND late_entry", Long.class,
                sheetId, id).isEmpty();
    }

    /** The ids of the sheet's late-entry claims. */
    public Set<Long> lateEntries(String sheetId) {
        return new HashSet<>(jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND late_entry", Long.class,
                sheetId));
    }

    /** Store the day the claim was last reminded. */
    public void remind(String sheetId, long id, LocalDate day) {
        jdbc.update("UPDATE claim SET reminded_on = ? WHERE id = ? AND sheet_id = ?", Date.valueOf(day), id, sheetId);
    }

    /** The day the claim was last reminded, or null. */
    public LocalDate reminded(String sheetId, long id) {
        List<Date> found = jdbc.queryForList("SELECT reminded_on FROM claim WHERE sheet_id = ? AND id = ?", Date.class,
                sheetId, id);
        return found.isEmpty() || found.get(0) == null ? null : found.get(0).toLocalDate();
    }

    /** The day each of the sheet's reminded claims was last reminded, by claim id. */
    public Map<Long, LocalDate> remindedAll(String sheetId) {
        Map<Long, LocalDate> reminded = new HashMap<>();
        jdbc.query("SELECT id, reminded_on FROM claim WHERE sheet_id = ? AND reminded_on IS NOT NULL",
                (ResultSet rs) -> {
                    reminded.put(rs.getLong("id"), rs.getDate("reminded_on").toLocalDate());
                }, sheetId);
        return reminded;
    }

    /** Store the claim's external reference (null: none). */
    public void saveRef(Claim claim, String ref) {
        jdbc.update("UPDATE claim SET external_ref = ?, external_ref_key = ? WHERE id = ? AND sheet_id = ?", ref,
                ref == null ? null : ref.toLowerCase(Locale.ROOT), claim.id(), claim.sheetId());
    }

    /** Whether a claim on the sheet other than the one with id {@code except} (null: none) has the reference, ignoring case. */
    public boolean refUsed(String sheetId, String ref, Long except) {
        return !jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND external_ref_key = ? AND id <> ?",
                Long.class, sheetId, ref.toLowerCase(Locale.ROOT), except == null ? -1L : except).isEmpty();
    }

    /** The claim's external reference, or null. */
    public String ref(String sheetId, long id) {
        List<String> found = jdbc.queryForList("SELECT external_ref FROM claim WHERE sheet_id = ? AND id = ?",
                String.class, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The external reference of each of the sheet's claims that has one, by claim id. */
    public Map<Long, String> refs(String sheetId) {
        Map<Long, String> refs = new HashMap<>();
        jdbc.query("SELECT id, external_ref FROM claim WHERE sheet_id = ? AND external_ref IS NOT NULL",
                (ResultSet rs) -> {
                    refs.put(rs.getLong("id"), rs.getString("external_ref"));
                }, sheetId);
        return refs;
    }

    /** Store the reason the claim was voided with (null: none). */
    public void saveVoidReason(Claim claim, String reason) {
        jdbc.update("UPDATE claim SET void_reason = ? WHERE id = ? AND sheet_id = ?", reason, claim.id(),
                claim.sheetId());
    }

    /** The reason the claim was voided with, or null. */
    public String voidReason(String sheetId, long id) {
        List<String> found = jdbc.queryForList("SELECT void_reason FROM claim WHERE sheet_id = ? AND id = ?",
                String.class, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The reason each of the sheet's claims voided with one was voided with, by claim id. */
    public Map<Long, String> voidReasons(String sheetId) {
        Map<Long, String> reasons = new HashMap<>();
        jdbc.query("SELECT id, void_reason FROM claim WHERE sheet_id = ? AND void_reason IS NOT NULL",
                (ResultSet rs) -> {
                    reasons.put(rs.getLong("id"), rs.getString("void_reason"));
                }, sheetId);
        return reasons;
    }

    /** Store the claim's tags (stored form; null: none), adding each to the sheet's tag list. */
    public void saveTags(Claim claim, String tags) {
        jdbc.update("UPDATE claim SET tags = ? WHERE id = ? AND sheet_id = ?", tags, claim.id(), claim.sheetId());
        if (tags != null) {
            for (String tag : tags.split(",")) {
                jdbc.update("MERGE INTO claim_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", claim.sheetId(), tag);
            }
        }
    }

    /** The claim's tags (stored form), or null. */
    public String tags(String sheetId, long id) {
        List<String> found = jdbc.queryForList("SELECT tags FROM claim WHERE sheet_id = ? AND id = ?", String.class,
                sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The tags (stored form) of each of the sheet's claims that has any, by claim id. */
    public Map<Long, String> tagsAll(String sheetId) {
        Map<Long, String> tags = new HashMap<>();
        jdbc.query("SELECT id, tags FROM claim WHERE sheet_id = ? AND tags IS NOT NULL", (ResultSet rs) -> {
            tags.put(rs.getLong("id"), rs.getString("tags"));
        }, sheetId);
        return tags;
    }

    /** Every tag ever given to a claim on the sheet, sorted. */
    public List<String> usedTags(String sheetId) {
        return jdbc.queryForList("SELECT tag FROM claim_tag WHERE sheet_id = ? ORDER BY tag", String.class, sheetId);
    }

    private static Claim map(ResultSet rs, int n) throws SQLException {
        return new Claim(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                rs.getBigDecimal("amount").setScale(2), rs.getDate("claim_date").toLocalDate(), rs.getString("note"),
                ClaimStatus.valueOf(rs.getString("status")), rs.getString("claim_number"),
                rs.getBigDecimal("due"), rs.getString("opened_by"), rs.getBoolean("stated"), rs.getBigDecimal("stated_due"),
                rs.getString("corrects"), rs.getDate("due_by").toLocalDate(),
                rs.getDate("deferred_until") == null ? null : rs.getDate("deferred_until").toLocalDate(),
                rs.getInt("proofs"), rs.getString("split_from"));
    }
}
