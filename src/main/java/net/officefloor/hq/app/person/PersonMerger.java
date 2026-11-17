package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.StatementRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Stores a merge of one person into another on the sheet (see #merge). */
@Repository
public class PersonMerger {

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;
    private final StatementRepository statements;
    private final MergeUndoer undoer;

    public PersonMerger(JdbcTemplate jdbc, SheetRepository sheets, StatementRepository statements, MergeUndoer undoer) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.statements = statements;
        this.undoer = undoer;
    }

    /**
     * Merges {@code from} into {@code into}: from's claims move to into (numbers and dates kept, listed after every claim
     * on the sheet in the order they were added, each keeping the figure a statement bound it to); from's statements move to
     * into, before into's own (into's count becomes their own plus from's; into's last statement stays theirs, or having
     * none becomes from's last); from's name and aliases become
     * into's aliases; from's patrons are added to into's (not into themselves, no repeats) and anyone naming from as
     * patron names into instead (no repeats; into never their own patron); from's pool memberships move to into (shares
     * added where into is already a member). From's holds, watchers, contact and grades are dropped, and from
     * leaves the sheet (kept, as they stood, only for reading the sheet as of a date before the merge). The merge is
     * kept, with what it moved and removed, for {@link MergeUndoer} to undo.
     */
    public void merge(String sheetId, Person from, Person into, String by) {
        long f = from.id();
        long t = into.id();
        List<Long> claims = jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed, id",
                Long.class, sheetId, f);
        long mergeId = undoer.keep(sheetId, f, t, by, claims);
        for (Long id : claims) {
            jdbc.update("UPDATE claim SET person_id = ?, "
                    + "listed = (SELECT COALESCE(MAX(c.listed), 0) + 1 FROM claim c WHERE c.sheet_id = ?) WHERE id = ?",
                    t, sheetId, id);
        }

        statements.moveAll(sheetId, f, sheetId, t);

        // the figures fixed for them by a (soft) close are added to into's (moved where into has none)
        for (Map<String, Object> p : jdbc.queryForList("SELECT period_id, total, carried FROM period_person_total"
                + " WHERE sheet_id = ? AND person_id = ?", sheetId, f)) {
            Object period = p.get("period_id");
            Integer theirs = jdbc.queryForObject("SELECT COUNT(*) FROM period_person_total WHERE sheet_id = ? AND period_id = ?"
                    + " AND person_id = ?", Integer.class, sheetId, period, t);
            if (theirs != null && theirs > 0) {
                jdbc.update("UPDATE period_person_total SET total = total + ?, carried = CASE WHEN carried IS NULL AND ? IS NULL"
                        + " THEN NULL ELSE COALESCE(carried, 0) + COALESCE(CAST(? AS DECIMAL(12, 2)), 0) END"
                        + " WHERE sheet_id = ? AND period_id = ? AND person_id = ?",
                        p.get("total"), p.get("carried"), p.get("carried"), sheetId, period, t);
                jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_id = ? AND person_id = ?",
                        sheetId, period, f);
            } else {
                jdbc.update("UPDATE period_person_total SET person_id = ? WHERE sheet_id = ? AND period_id = ? AND person_id = ?",
                        t, sheetId, period, f);
            }
        }

        jdbc.update("UPDATE person_alias SET person_id = ? WHERE sheet_id = ? AND person_id = ?", t, sheetId, f);

        for (Map<String, Object> p : jdbc.queryForList("SELECT patron_id, added_on FROM patron WHERE sheet_id = ? AND person_id = ?",
                sheetId, f)) {
            addPatron(sheetId, t, ((Number) p.get("patron_id")).longValue(), p.get("added_on"));
        }
        for (Map<String, Object> p : jdbc.queryForList("SELECT person_id, added_on FROM patron WHERE sheet_id = ? AND patron_id = ?",
                sheetId, f)) {
            addPatron(sheetId, ((Number) p.get("person_id")).longValue(), t, p.get("added_on"));
        }
        jdbc.update("DELETE FROM patron WHERE sheet_id = ? AND (person_id = ? OR patron_id = ?)", sheetId, f, f);

        for (Map<String, Object> m : jdbc.queryForList("SELECT id, pool_id, share FROM pool_member WHERE person_id = ? ORDER BY id", f)) {
            long poolId = ((Number) m.get("pool_id")).longValue();
            BigDecimal share = (BigDecimal) m.get("share");
            List<Long> theirs = jdbc.queryForList("SELECT id FROM pool_member WHERE pool_id = ? AND person_id = ?", Long.class,
                    poolId, t);
            if (theirs.isEmpty()) {
                jdbc.update("UPDATE pool_member SET person_id = ? WHERE id = ?", t, m.get("id"));
            } else {
                jdbc.update("UPDATE pool_member SET share = share + ? WHERE id = ?", share, theirs.get(0));
                jdbc.update("DELETE FROM pool_member WHERE id = ?", m.get("id"));
            }
        }

        // the crews they lead are led by the person kept (no longer a member of them); their memberships move to them;
        // a membership removed is kept, as it stood, for reading the crews as of a date before the merge
        String kept = " WHERE person_id = ? AND crew_id IN (SELECT id FROM crew WHERE lead_id = ?)";
        leaveCrews(sheetId, kept, t, f);
        jdbc.update("DELETE FROM crew_member" + kept, t, f);
        jdbc.update("UPDATE crew SET lead_id = ? WHERE lead_id = ?", t, f);
        String gone = " WHERE person_id = ? AND (crew_id IN (SELECT id FROM crew WHERE lead_id = ?)"
                + " OR crew_id IN (SELECT crew_id FROM crew_member WHERE person_id = ?))";
        leaveCrews(sheetId, gone, f, t, t);
        jdbc.update("DELETE FROM crew_member" + gone, f, t, t);
        jdbc.update("UPDATE crew_member SET person_id = ? WHERE person_id = ?", t, f);

        StringBuilder grades = new StringBuilder();
        for (PersonGrade g : from.grades()) {
            grades.append(grades.length() == 0 ? "" : ";").append(g.grade()).append('@').append(g.from() == null ? "" : g.from());
        }
        jdbc.update("INSERT INTO person_merged (id, sheet_id, name, grades, retired_from, terms, contact, added_on, merged_on)"
                + " SELECT id, sheet_id, name, ?, retired_from, terms, contact, added_on, ? FROM person WHERE sheet_id = ? AND id = ?",
                grades.toString(), java.sql.Date.valueOf(sheets.today(sheetId)), sheetId, f);

        for (String table : List.of("person_hold", "watch", "person_approver", "person_grade", "person_grade_log",
                "person_retire_log", "person_retirement", "person_terms_log", "person_terms_from")) {
            jdbc.update("DELETE FROM " + table + " WHERE sheet_id = ? AND person_id = ?", sheetId, f);
        }
        jdbc.update("DELETE FROM person WHERE sheet_id = ? AND id = ?", sheetId, f);
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias, added_on) VALUES (?, ?, ?, ?)", sheetId, t, from.name(),
                java.sql.Date.valueOf(sheets.today(sheetId)));
        undoer.keepNameAlias(mergeId, sheetId, t, from.name());
    }

    /** Keeps the crew memberships the condition picks as they stood, as left on the sheet's today. */
    private void leaveCrews(String sheetId, String where, Object... args) {
        java.sql.Date today = java.sql.Date.valueOf(sheets.today(sheetId));
        for (Map<String, Object> m : jdbc.queryForList("SELECT id, crew_id, person_id, made_on FROM crew_member" + where, args)) {
            jdbc.update("INSERT INTO crew_member_left (id, crew_id, person_id, name, made_on, left_on)"
                    + " SELECT ?, ?, id, name, ?, ? FROM person WHERE id = ?", m.get("id"), m.get("crew_id"), m.get("made_on"),
                    today, m.get("person_id"));
        }
    }

    /** Names the patron for the person, unless it is themselves or they already have them. */
    private void addPatron(String sheetId, long personId, long patronId, Object addedOn) {
        if (personId == patronId) {
            return;
        }
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM patron WHERE sheet_id = ? AND person_id = ? AND patron_id = ?",
                Integer.class, sheetId, personId, patronId);
        if (n != null && n == 0) {
            jdbc.update("INSERT INTO patron (sheet_id, person_id, patron_id, added_on) VALUES (?, ?, ?, ?)", sheetId, personId,
                    patronId, addedOn);
        }
    }
}
