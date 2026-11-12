package net.officefloor.hq.app.role;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.SheetStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The steward role delegated per sheet. A delegation is in force while the sheet's today is within
 *  its from..to, inclusive; the user then acts as a steward in every rule. */
@Repository
public class DelegationStore {

    private static final RowMapper<Delegation> ROW = (rs, i) -> new Delegation(rs.getString("user_name"),
            rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class),
            rs.getString("delegated_by"));

    private final JdbcTemplate jdbc;
    private final SheetStore sheets;

    public DelegationStore(JdbcTemplate jdbc, SheetStore sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** Every delegation on the sheet, in the order they were made. */
    public List<Delegation> list(String sheetId) {
        return jdbc.query("SELECT user_name, from_date, to_date, delegated_by FROM delegation WHERE sheet_id = ?"
                + " ORDER BY id", ROW, sheetId);
    }

    /** The steward who delegated to the user a delegation in force on the sheet's today (the latest made
     *  when several are), or null when none is. */
    public String delegator(String sheetId, String user) {
        if (user == null) {
            return null;
        }
        LocalDate today = sheets.today(sheetId);
        List<String> found = jdbc.queryForList("SELECT delegated_by FROM delegation WHERE sheet_id = ?"
                + " AND user_name = ? AND from_date <= ? AND to_date >= ? ORDER BY id DESC", String.class,
                sheetId, user.trim(), today, today);
        return found.isEmpty() ? null : found.get(0);
    }

    public void add(String sheetId, Delegation d) {
        jdbc.update("INSERT INTO delegation (sheet_id, user_name, from_date, to_date, delegated_by)"
                + " VALUES (?, ?, ?, ?, ?)", sheetId, d.user(), d.from(), d.to(), d.by());
    }
}
