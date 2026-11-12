package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The pools per sheet, and their members: people on the sheet, each with a share percent. */
@Repository
public class PoolStore {

    /** A pool as stored. */
    public record Pool(long id, String name) {
    }

    /** A member of a pool: the person, their share, and whether they are retired (a retirement date set). */
    public record Member(long poolId, long personId, String name, BigDecimal share, boolean retired) {
    }

    private final JdbcTemplate jdbc;

    public PoolStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's pools, in the order they were added; as of the date when one is given (see {@link History}). */
    public List<Pool> list(String sheetId, LocalDate on) {
        String pool = on == null ? "pool" : History.asOf("pool", "id", on);
        return jdbc.query("SELECT id, name FROM " + pool + " WHERE sheet_id = ? ORDER BY id",
                (rs, i) -> new Pool(rs.getLong("id"), rs.getString("name")), sheetId);
    }

    /** The members of the sheet's pools, by pool id, each pool's in the order they were added. */
    public Map<Long, List<Member>> members(String sheetId, LocalDate on) {
        String member = on == null ? "pool_member" : History.asOf("pool_member", "id", on);
        String person = on == null ? "person" : History.asOf("person", "id", on);
        Map<Long, List<Member>> found = new LinkedHashMap<>();
        jdbc.query("SELECT pool_member.pool_id, pool_member.person_id, person.name, pool_member.share,"
                + " person.retired_from FROM " + member + " JOIN " + person + " ON person.id = pool_member.person_id"
                + " WHERE pool_member.sheet_id = ? ORDER BY pool_member.id", rs -> {
                    Member m = new Member(rs.getLong("pool_id"), rs.getLong("person_id"), rs.getString("name"),
                            rs.getBigDecimal("share"), rs.getObject("retired_from") != null);
                    found.computeIfAbsent(m.poolId(), k -> new java.util.ArrayList<>()).add(m);
                }, sheetId);
        return found;
    }

    public Pool find(String sheetId, long id) {
        List<Pool> found = jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? AND id = ?",
                (rs, i) -> new Pool(rs.getLong("id"), rs.getString("name")), sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public boolean has(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?", Integer.class,
                sheetId, name);
        return n != null && n > 0;
    }

    public void add(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
    }

    public boolean isMember(long poolId, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE pool_id = ? AND person_id = ?",
                Integer.class, poolId, personId);
        return n != null && n > 0;
    }

    public void addMember(String sheetId, long poolId, long personId, BigDecimal share) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, person_id, share) VALUES (?, ?, ?, ?)", sheetId,
                poolId, personId, share);
    }
}
