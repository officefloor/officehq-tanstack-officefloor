package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** People by sheet, in the order they were added. */
@Repository
public class PersonRepository {

    private static final RowMapper<Person> ROW = (rs, i) -> new Person(rs.getLong("id"),
            rs.getString("sheet_id"), rs.getString("name"), rs.getString("grade"), rs.getLong("version"),
            rs.getInt("statement_count"), rs.getBigDecimal("stated_total"), rs.getObject("statement_date", LocalDate.class));

    private final JdbcTemplate jdbc;

    public PersonRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> list(String sheetId) {
        return jdbc.query("SELECT * FROM person WHERE sheet_id = ? ORDER BY id", ROW, sheetId);
    }

    /** The person on this sheet, or null when absent. */
    public Person find(String sheetId, long id) {
        return jdbc.query("SELECT * FROM person WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    /** Whether another person on this sheet (other than exceptId, when given) has this name,
     *  compared without regard to case and ignoring surrounding spaces. */
    public boolean nameInUse(String sheetId, String name, Long exceptId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM person WHERE sheet_id = ? AND name_key = LOWER(TRIM(?)) AND id <> ?",
                Integer.class, sheetId, name, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    public Person insert(String sheetId, String name, String grade) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO person (sheet_id, name, grade) VALUES (?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setString(3, grade);
            return ps;
        }, keys);
        return new Person(keys.getKey().longValue(), sheetId, name, grade, 0, 0, null, null);
    }

    /** Lock the person's row until the transaction ends, so concurrent statements of them go one after the other. */
    public void lock(Person person) {
        jdbc.query("SELECT id FROM person WHERE sheet_id = ? AND id = ? FOR UPDATE", rs -> null,
                person.sheetId(), person.id());
    }

    /** Record a statement taken of the person: one more taken, with its due-total (null: n/a) and date. Leaves the version alone: a statement is not an edit of the person. */
    public void stated(Person person, BigDecimal total, LocalDate date) {
        jdbc.update("UPDATE person SET statement_count = statement_count + 1, stated_total = ?, statement_date = ? "
                + "WHERE sheet_id = ? AND id = ?", total, date, person.sheetId(), person.id());
    }

    /** Store the person at its new version, only while the stored person is still at version
     *  `from`; false (nothing written) when it has been saved since. */
    public boolean update(Person person, long from) {
        return jdbc.update("UPDATE person SET name = ?, grade = ?, version = ? WHERE sheet_id = ? AND id = ? AND version = ?",
                person.name(), person.grade(), person.version(), person.sheetId(), person.id(), from) == 1;
    }
}
