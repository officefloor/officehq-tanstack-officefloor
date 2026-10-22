package net.officefloor.hq.app.pool;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The pools by sheet, and their members. Each is kept with the sheet's today when it was added (added_on). */
@Repository
public class PoolRepository {

    private static final RowMapper<Pool> ROW = (rs, i) -> new Pool(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getString("name"), rs.getString("added_by"));

    private final JdbcTemplate jdbc;

    public PoolRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's pools in the order they were added (those added by the end of the day, when one is given). */
    public List<Pool> list(String sheetId, LocalDate end) {
        return end == null
                ? jdbc.query("SELECT * FROM pool WHERE sheet_id = ? ORDER BY id", ROW, sheetId)
                : jdbc.query("SELECT * FROM pool WHERE sheet_id = ? AND (added_on IS NULL OR added_on <= ?) "
                        + "ORDER BY id", ROW, sheetId, end);
    }

    /** The pool on this sheet with this name, or null when absent. */
    public Pool find(String sheetId, String name) {
        return jdbc.query("SELECT * FROM pool WHERE sheet_id = ? AND name = ?", ROW, sheetId, name).stream()
                .findFirst().orElse(null);
    }

    /** The pool on this sheet with this id, or null when absent. */
    public Pool find(String sheetId, long id) {
        return jdbc.query("SELECT * FROM pool WHERE sheet_id = ? AND id = ?", ROW, sheetId, id).stream()
                .findFirst().orElse(null);
    }

    public Pool add(String sheetId, PoolInput input) {
        jdbc.update("INSERT INTO pool (sheet_id, name, added_by, added_on) "
                + "VALUES (?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", sheetId, input.name(), input.user(),
                sheetId);
        return find(sheetId, input.name());
    }

    public boolean isMember(Pool pool, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE pool_id = ? AND person_id = ?",
                Integer.class, pool.id(), personId);
        return n != null && n > 0;
    }

    public PoolMember addMember(Pool pool, Person person, PoolMemberInput input) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, person_id, share, added_by, added_on) "
                + "VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", pool.sheetId(), pool.id(),
                person.id(), input.share(), input.user(), pool.sheetId());
        return new PoolMember(pool.id(), pool.sheetId(), pool.name(), person.id(), person.name(), input.share(),
                input.user());
    }

    /** The pool's members in the order they were added (those added by the end of the day, when one is given),
     *  named by the person's id only: the caller names them. */
    public List<PoolMember> members(Pool pool, LocalDate end) {
        RowMapper<PoolMember> row = (rs, i) -> new PoolMember(pool.id(), pool.sheetId(), pool.name(),
                rs.getLong("person_id"), null, rs.getBigDecimal("share").setScale(2), rs.getString("added_by"));
        return end == null
                ? jdbc.query("SELECT * FROM pool_member WHERE pool_id = ? ORDER BY id", row, pool.id())
                : jdbc.query("SELECT * FROM pool_member WHERE pool_id = ? AND (added_on IS NULL OR added_on <= ?) "
                        + "ORDER BY id", row, pool.id(), end);
    }
}
