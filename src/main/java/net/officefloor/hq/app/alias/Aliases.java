package net.officefloor.hq.app.alias;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The aliases of the people on a sheet, each unique on it ignoring case. */
@Repository
public class Aliases {

    private static final RowMapper<Alias> ROW = (rs, i) -> new Alias(rs.getLong("person_id"), rs.getString("alias"));

    private final JdbcTemplate jdbc;

    public Aliases(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every alias on the sheet. */
    public List<Alias> list(String sheet) {
        return jdbc.query("SELECT person_id, alias FROM person_alias WHERE sheet = ? ORDER BY person_id, alias_key",
                ROW, sheet);
    }

    /** Whether any person on the sheet has this alias, ignoring case. */
    public boolean taken(String sheet, String alias) {
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person_alias WHERE sheet = ? AND alias_key = LOWER(?)", Integer.class, sheet,
                alias);
        return found != null && found > 0;
    }

    public void add(String sheet, long person, String alias) {
        jdbc.update("INSERT INTO person_alias (sheet, person_id, alias) VALUES (?, ?, ?)", sheet, person, alias);
    }
}
