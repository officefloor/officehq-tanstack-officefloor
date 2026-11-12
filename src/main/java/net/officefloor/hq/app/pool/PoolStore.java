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

    /** A member of a pool: the person, or the pool (personId null, memberPoolId set), the name, the share,
     *  whether the person is retired (a retirement date set; never for a pool), and the dates the membership
     *  counts from and to (null for no bound). */
    public record Member(long poolId, Long personId, Long memberPoolId, String name, BigDecimal share,
            boolean retired, LocalDate from, LocalDate to) {

        /** Whether the membership's dates include the day. */
        public boolean covers(LocalDate day) {
            return (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to));
        }
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
        String pool = on == null ? "pool" : History.asOf("pool", "id", on);
        Map<Long, List<Member>> found = new LinkedHashMap<>();
        jdbc.query("SELECT pool_member.pool_id, pool_member.person_id, pool_member.member_pool_id, person.name,"
                + " pool.name AS pool_name, pool_member.share, person.retired_from, pool_member.from_date,"
                + " pool_member.to_date FROM " + member
                + " LEFT JOIN " + person + " ON person.id = pool_member.person_id"
                + " LEFT JOIN " + pool + " ON pool.id = pool_member.member_pool_id"
                + " WHERE pool_member.sheet_id = ? AND (person.id IS NOT NULL OR pool.id IS NOT NULL)"
                + " ORDER BY pool_member.id", rs -> {
                    Long personId = rs.getObject("person_id") == null ? null : rs.getLong("person_id");
                    Long memberPoolId = rs.getObject("member_pool_id") == null ? null : rs.getLong("member_pool_id");
                    Member m = new Member(rs.getLong("pool_id"), personId, memberPoolId,
                            personId != null ? rs.getString("name") : rs.getString("pool_name"),
                            rs.getBigDecimal("share"), rs.getObject("retired_from") != null,
                            date(rs.getDate("from_date")), date(rs.getDate("to_date")));
                    found.computeIfAbsent(m.poolId(), k -> new java.util.ArrayList<>()).add(m);
                }, sheetId);
        return found;
    }

    private static LocalDate date(java.sql.Date d) {
        return d == null ? null : d.toLocalDate();
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

    public void addMember(String sheetId, long poolId, long personId, BigDecimal share, LocalDate from,
            LocalDate to) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, person_id, share, from_date, to_date)"
                + " VALUES (?, ?, ?, ?, ?, ?)", sheetId, poolId, personId, share, from, to);
    }

    /** Adds the pool {@code memberPoolId} as a member of the pool {@code poolId}. */
    public void addPoolMember(String sheetId, long poolId, long memberPoolId, BigDecimal share, LocalDate from,
            LocalDate to) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, member_pool_id, share, from_date, to_date)"
                + " VALUES (?, ?, ?, ?, ?, ?)", sheetId, poolId, memberPoolId, share, from, to);
    }

    /** The sheet's pools that are members of another, each to the pool it is a member of. */
    public Map<Long, Long> parents(String sheetId) {
        Map<Long, Long> found = new LinkedHashMap<>();
        jdbc.query("SELECT pool_id, member_pool_id FROM pool_member WHERE sheet_id = ? AND member_pool_id IS NOT NULL",
                rs -> {
                    found.put(rs.getLong("member_pool_id"), rs.getLong("pool_id"));
                }, sheetId);
        return found;
    }

    /** Moves the memberships of {@code fromId} to {@code intoId}, adding the share to theirs in a pool they are
     *  already in. */
    public void merge(String sheetId, long fromId, long intoId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, pool_id, share FROM pool_member WHERE sheet_id = ? AND person_id = ? ORDER BY id", sheetId, fromId);
        for (Map<String, Object> row : rows) {
            long poolId = ((Number) row.get("POOL_ID")).longValue();
            long id = ((Number) row.get("ID")).longValue();
            if (isMember(poolId, intoId)) {
                jdbc.update("UPDATE pool_member SET share = share + ? WHERE pool_id = ? AND person_id = ?",
                        row.get("SHARE"), poolId, intoId);
                jdbc.update("DELETE FROM pool_member WHERE id = ?", id);
            } else {
                jdbc.update("UPDATE pool_member SET person_id = ? WHERE id = ?", intoId, id);
            }
        }
    }

    /** Drops the person's pool memberships. */
    public void drop(String sheetId, long personId) {
        jdbc.update("DELETE FROM pool_member WHERE sheet_id = ? AND person_id = ?", sheetId, personId);
    }
}
