package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/** The store of the sheet's pools and their members. */
@Repository
public class Pools {

    /** A stored member: the pool's name, the person's id (0 for a pool), the share and the member pool (null for a person). */
    public record Membership(String pool, long personId, BigDecimal share, String memberPool) {
    }

    private static final String NEXT_LISTED = "(SELECT COALESCE(MAX(listed), 0) + 1 FROM (SELECT listed FROM pool_member "
            + "WHERE sheet_id = ? UNION ALL SELECT listed FROM pool_nested WHERE sheet_id = ?) l)";

    private final JdbcTemplate jdbc;

    public Pools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's pool names, in the order they were added. */
    public List<String> names(String sheetId) {
        return jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ? ORDER BY listed", String.class, sheetId);
    }

    public boolean has(String sheetId, String name) {
        return names(sheetId).contains(name);
    }

    public void add(Pool pool) {
        jdbc.update("INSERT INTO pool (sheet_id, name, listed) VALUES (?, ?, "
                + "(SELECT COALESCE(MAX(listed), 0) + 1 FROM pool WHERE sheet_id = ?))",
                pool.sheetId(), pool.name(), pool.sheetId());
    }

    /** The sheet's pool members, people and pools, in the order they were added. */
    public List<Membership> members(String sheetId) {
        return jdbc.query("SELECT pool_name, person_id, share, CAST(NULL AS VARCHAR(255)) AS member_pool, listed "
                + "FROM pool_member WHERE sheet_id = ? UNION ALL SELECT pool_name, 0, share, member_pool, listed "
                + "FROM pool_nested WHERE sheet_id = ? ORDER BY listed",
                (rs, n) -> new Membership(rs.getString("pool_name"), rs.getLong("person_id"),
                        rs.getBigDecimal("share").setScale(2), rs.getString("member_pool")),
                sheetId, sheetId);
    }

    /** Each pool that is a member of another, to the pool it is a member of. */
    public Map<String, String> parents(String sheetId) {
        Map<String, String> out = new HashMap<>();
        jdbc.query("SELECT pool_name, member_pool FROM pool_nested WHERE sheet_id = ?",
                (RowCallbackHandler) rs -> out.put(rs.getString("member_pool"), rs.getString("pool_name")), sheetId);
        return out;
    }

    public boolean isMember(String sheetId, String pool, long personId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pool_member WHERE sheet_id = ? AND pool_name = ? AND person_id = ?", Integer.class,
                sheetId, pool, personId);
        return n != null && n > 0;
    }

    public void addMember(PoolMember member) {
        if (member.memberPool() != null) {
            jdbc.update("INSERT INTO pool_nested (sheet_id, pool_name, member_pool, share, listed) VALUES (?, ?, ?, ?, "
                    + NEXT_LISTED + ")", member.sheetId(), member.pool(), member.memberPool(), member.share(),
                    member.sheetId(), member.sheetId());
            return;
        }
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, share, listed) VALUES (?, ?, ?, ?, "
                + NEXT_LISTED + ")", member.sheetId(), member.pool(), member.personId(), member.share(),
                member.sheetId(), member.sheetId());
    }
}
