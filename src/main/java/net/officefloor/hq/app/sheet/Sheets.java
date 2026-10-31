package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The sheets and their "today". A sheet starts with the real date the first time it is loaded;
 * from then on {@link #today(String)} is the only date the app uses for that sheet — never the
 * real clock.
 */
@Repository
public class Sheets {

    private final JdbcTemplate jdbc;

    public Sheets(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's today, starting the sheet at the real date when it has never been used. */
    public LocalDate today(String sheetId) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) SELECT ?, ? FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM sheet WHERE id = ?)",
                sheetId, Date.valueOf(LocalDate.now()), sheetId);
        return jdbc.queryForObject("SELECT today FROM sheet WHERE id = ?", Date.class, sheetId)
                .toLocalDate();
    }

    /**
     * The sheet's next claim number: the next after the last number given anywhere in the sheet's
     * tree (see {@link #treeOf}), so the sheets of a tree share one sequence; 1 for a tree's first.
     * The rows of the tree's sheets stay locked until the transaction ends, so two claims opened at
     * once get different numbers, and a number is never given twice.
     */
    public long nextClaimNumber(String sheetId) {
        today(sheetId);
        java.util.List<String> tree = new java.util.ArrayList<>(treeOf(sheetId));
        java.util.Collections.sort(tree);
        long last = 0;
        for (String sheet : tree) {
            jdbc.update("UPDATE sheet SET claim_numbers = claim_numbers WHERE id = ?", sheet);
            Long given = jdbc.queryForList("SELECT claim_numbers FROM sheet WHERE id = ?", Long.class, sheet)
                    .stream().findFirst().orElse(0L);
            last = Math.max(last, given == null ? 0 : given);
        }
        jdbc.update("UPDATE sheet SET claim_numbers = ? WHERE id = ?", last + 1, sheetId);
        return last + 1;
    }

    /** The sheets of the sheet's tree, the sheet among them: its root (the top of its parent chain)
     *  and every sheet below the root (its children, their children, and so on). Not dated: read
     *  from the live sheets. */
    private java.util.Set<String> treeOf(String sheetId) {
        String root = sheetId;
        java.util.Set<String> up = new java.util.HashSet<>(java.util.List.of(sheetId));
        for (String p = parentOf(sheetId); p != null && up.add(p); p = parentOf(p)) {
            root = p;
        }
        java.util.Set<String> tree = new java.util.LinkedHashSet<>(java.util.List.of(root));
        java.util.ArrayDeque<String> todo = new java.util.ArrayDeque<>(java.util.List.of(root));
        while (!todo.isEmpty()) {
            for (String child : jdbc.queryForList("SELECT sheet_id FROM PUBLIC.sheet_setting WHERE parent_id = ?",
                    String.class, todo.poll())) {
                if (tree.add(child)) todo.add(child);
            }
        }
        tree.add(sheetId);
        return tree;
    }

    private String parentOf(String sheetId) {
        return jdbc.queryForList("SELECT parent_id FROM PUBLIC.sheet_setting WHERE sheet_id = ?", String.class, sheetId)
                .stream().filter(p -> p != null && !p.isBlank()).findFirst().orElse(null);
    }

    /**
     * The next number of one of the sheet's period sequences (a period's name, or "X" for claims in
     * no period): 1 for its first, then 2, and so on. Locks the sheet's row as {@link #nextClaimNumber}
     * does, so a number is never given twice.
     */
    public long nextPeriodNumber(String sheetId, String seq) {
        today(sheetId);
        jdbc.update("UPDATE sheet SET claim_numbers = claim_numbers WHERE id = ?", sheetId);
        jdbc.update("MERGE INTO period_number (sheet_id, seq, given) KEY (sheet_id, seq) "
                + "VALUES (?, ?, COALESCE((SELECT given FROM period_number WHERE sheet_id = ? AND seq = ?), 0) + 1)",
                sheetId, seq, sheetId, seq);
        return jdbc.queryForObject("SELECT given FROM period_number WHERE sheet_id = ? AND seq = ?", Long.class, sheetId, seq);
    }

    /** Whether the sheet has been loaded (used) at least once. */
    public boolean exists(String sheetId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId) > 0;
    }

    /** Whether a steward has archived the sheet: then every change to it is refused. */
    public boolean archived(String sheetId) {
        today(sheetId);
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT archived FROM sheet WHERE id = ?", Boolean.class, sheetId));
    }

    public void archive(String sheetId) {
        today(sheetId);
        jdbc.update("UPDATE sheet SET archived = TRUE, archived_on = today WHERE id = ?", sheetId);
    }

    public void setToday(String sheetId, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", sheetId, Date.valueOf(today));
    }
}
