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

    /** The pool this pool is a member of, or null when it is a member of none. */
    public Pool parent(Pool pool) {
        return jdbc.query("SELECT p.* FROM pool p JOIN pool_member m ON m.pool_id = p.id WHERE m.member_pool_id = ?",
                ROW, pool.id()).stream().findFirst().orElse(null);
    }

    /** The pools that are members of this pool. */
    public List<Pool> children(Pool pool) {
        return jdbc.query("SELECT p.* FROM pool p JOIN pool_member m ON m.member_pool_id = p.id WHERE m.pool_id = ? "
                + "ORDER BY m.id", ROW, pool.id());
    }

    public PoolMember addMember(Pool pool, Pool member, PoolMemberInput input) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, member_pool_id, share, added_by, added_on, from_date, "
                + "to_date) VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?), ?, ?)", pool.sheetId(),
                pool.id(), member.id(), input.share(), input.user(), pool.sheetId(), input.from(), input.to());
        return new PoolMember(pool.id(), pool.sheetId(), pool.name(), 0, member.name(), input.share(), input.user(),
                member.id(), input.from(), input.to());
    }

    public PoolMember addMember(Pool pool, Person person, PoolMemberInput input) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, person_id, share, added_by, added_on, from_date, "
                + "to_date) VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?), ?, ?)", pool.sheetId(),
                pool.id(), person.id(), input.share(), input.user(), pool.sheetId(), input.from(), input.to());
        return new PoolMember(pool.id(), pool.sheetId(), pool.name(), person.id(), person.name(), input.share(),
                input.user(), null, input.from(), input.to());
    }

    /** The pool's members in the order they were added (those added by the end of the day, when one is given),
     *  named by the person's id (or the member pool's id) only: the caller names them. */
    public List<PoolMember> members(Pool pool, LocalDate end) {
        RowMapper<PoolMember> row = (rs, i) -> new PoolMember(pool.id(), pool.sheetId(), pool.name(),
                rs.getLong("person_id"), null, rs.getBigDecimal("share").setScale(2), rs.getString("added_by"),
                rs.getObject("member_pool_id", Long.class), rs.getObject("from_date", LocalDate.class),
                rs.getObject("to_date", LocalDate.class));
        return end == null
                ? jdbc.query("SELECT * FROM pool_member WHERE pool_id = ? ORDER BY id", row, pool.id())
                : jdbc.query("SELECT * FROM pool_member WHERE pool_id = ? AND (added_on IS NULL OR added_on <= ?) "
                        + "ORDER BY id", row, pool.id(), end);
    }

    /** Drop the person from every pool they are a member of. */
    public void drop(String sheetId, long personId) {
        jdbc.update("DELETE FROM pool_member WHERE sheet_id = ? AND person_id = ?", sheetId, personId);
    }

    /** On merging one person into another: the first's pool memberships move to the other, their share added to the
     *  other's where the other is already a member. */
    public void merge(String sheetId, long fromId, long intoId) {
        jdbc.update("UPDATE pool_member m SET share = share + (SELECT f.share FROM pool_member f WHERE f.pool_id = m.pool_id "
                + "AND f.person_id = ?) WHERE sheet_id = ? AND person_id = ? AND EXISTS (SELECT 1 FROM pool_member f "
                + "WHERE f.pool_id = m.pool_id AND f.person_id = ?)", fromId, sheetId, intoId, fromId);
        jdbc.update("DELETE FROM pool_member m WHERE sheet_id = ? AND person_id = ? AND EXISTS (SELECT 1 FROM pool_member i "
                + "WHERE i.pool_id = m.pool_id AND i.person_id = ?)", sheetId, fromId, intoId);
        jdbc.update("UPDATE pool_member SET person_id = ? WHERE sheet_id = ? AND person_id = ?", intoId, sheetId, fromId);
    }
}
