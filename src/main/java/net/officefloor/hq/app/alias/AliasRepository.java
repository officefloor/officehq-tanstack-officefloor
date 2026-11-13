package net.officefloor.hq.app.alias;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AliasRepository {

    private final JdbcTemplate jdbc;

    public AliasRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's people's aliases, by person and then in the order they were added. */
    public List<Alias> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, person_id, alias FROM person_alias WHERE sheet_id = ? ORDER BY person_id, id",
                (rs, n) -> new Alias(rs.getString("sheet_id"), rs.getLong("person_id"), rs.getString("alias")),
                sheetId);
    }

    public void save(Alias alias) {
        jdbc.update("INSERT INTO person_alias (sheet_id, person_id, alias) VALUES (?, ?, ?)", alias.sheetId(),
                alias.personId(), alias.alias());
    }
}
