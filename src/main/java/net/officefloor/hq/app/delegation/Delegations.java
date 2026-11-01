package net.officefloor.hq.app.delegation;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The steward role lent per sheet, kept in the order the delegations were made. */
@Repository
public class Delegations {

    private static final RowMapper<Delegation> ROW = (rs, i) -> new Delegation(rs.getLong("id"),
            rs.getString("delegator"), rs.getString("user_name"), rs.getDate("from_date").toLocalDate(),
            rs.getDate("to_date").toLocalDate());

    private final JdbcTemplate jdbc;

    public Delegations(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every delegation made on the sheet, oldest first. */
    public List<Delegation> list(String sheet) {
        return jdbc.query("SELECT * FROM delegation WHERE sheet = ? ORDER BY id", ROW, sheet);
    }

    /** The steward who lent the user the role on the given day, the latest delegation covering it; empty when none. */
    public Optional<String> delegatorOn(String sheet, String user, LocalDate day) {
        if (user == null) {
            return Optional.empty();
        }
        List<String> found = jdbc.queryForList("SELECT delegator FROM delegation WHERE sheet = ? AND user_name = ? "
                + "AND from_date <= ? AND to_date >= ? ORDER BY id DESC", String.class, sheet, user,
                Date.valueOf(day), Date.valueOf(day));
        return found.stream().findFirst();
    }

    public void add(String sheet, String by, String user, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO delegation (sheet, delegator, user_name, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                sheet, by, user, Date.valueOf(from), Date.valueOf(to));
    }
}
