package net.officefloor.hq.app.person;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AliasRepository {

    private final JdbcTemplate jdbc;

    public AliasRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every alias on the sheet, by person, in the order they were added. */
    public List<Alias> list(String sheetId) {
        return jdbc.query("SELECT person_id, alias FROM person_alias WHERE sheet_id = ? ORDER BY person_id, id",
                (rs, i) -> new Alias(rs.getLong("person_id"), rs.getString("alias")), sheetId);
    }

    public void add(String sheetId, long personId, String alias) {
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", sheetId, personId, alias);
    }
}
