package net.officefloor.hq.app.role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored delegations of the steward role, per sheet, in the order they were made. */
@Repository
public class DelegationRepository {

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;

    public DelegationRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** The sheet's delegations, in the order they were made. */
    public List<Delegation> list(String sheetId) {
        return jdbc.query("SELECT id, sheet_id, user_name, steward, from_date, to_date FROM delegation"
                + " WHERE sheet_id = ? ORDER BY id", DelegationRepository::map, sheetId);
    }

    public Delegation add(String sheetId, DelegationValues values) {
        jdbc.update("INSERT INTO delegation (sheet_id, user_name, steward, from_date, to_date, made_on) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, values.user(), values.by(), values.from(), values.to(), sheets.today(sheetId));
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM delegation WHERE sheet_id = ?", Long.class, sheetId);
        return new Delegation(id, sheetId, values.user(), values.by(), values.from().toString(),
                values.to().toString());
    }

    /** The steward the user acts for on the sheet's today: the steward of the first delegation made to the user whose
     *  range holds today; empty when none does. */
    public Optional<String> actingFor(String sheetId, String user) {
        if (user == null || user.isEmpty()) {
            return Optional.empty();
        }
        return actingFor(sheetId, user, sheets.today(sheetId));
    }

    /** The steward the user acts for on the date: as {@link #actingFor(String, String)}, the date for the sheet's today. */
    public Optional<String> actingFor(String sheetId, String user, LocalDate today) {
        if (user == null || user.isEmpty()) {
            return Optional.empty();
        }
        List<String> found = jdbc.queryForList("SELECT steward FROM delegation WHERE sheet_id = ? AND user_name = ?"
                + " AND from_date <= ? AND to_date >= ? ORDER BY id", String.class, sheetId, user, today, today);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    /** The steward the user acted for at the end of the date: as {@link #actingFor(String, String, LocalDate)}, counting
     *  only the delegations already made then (made while the sheet's today was on or before the date). */
    public Optional<String> actingForAsOf(String sheetId, String user, LocalDate asOf) {
        if (user == null || user.isEmpty()) {
            return Optional.empty();
        }
        List<String> found = jdbc.queryForList("SELECT steward FROM delegation WHERE sheet_id = ? AND user_name = ?"
                + " AND from_date <= ? AND to_date >= ? AND (made_on IS NULL OR made_on <= ?) ORDER BY id", String.class,
                sheetId, user, asOf, asOf, asOf);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    private static Delegation map(ResultSet rs, int n) throws SQLException {
        return new Delegation(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("user_name"),
                rs.getString("steward"), rs.getObject("from_date", LocalDate.class).toString(),
                rs.getObject("to_date", LocalDate.class).toString());
    }
}
