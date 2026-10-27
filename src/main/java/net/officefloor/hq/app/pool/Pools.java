package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of the sheet's pools and their members. */
@Repository
public class Pools {

    /** A stored member: the pool's name, the person's id and the share. */
    public record Membership(String pool, long personId, BigDecimal share) {
    }

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

    /** The sheet's pool members, in the order they were added. */
    public List<Membership> members(String sheetId) {
        return jdbc.query("SELECT pool_name, person_id, share FROM pool_member WHERE sheet_id = ? ORDER BY listed",
                (rs, n) -> new Membership(rs.getString("pool_name"), rs.getLong("person_id"),
                        rs.getBigDecimal("share").setScale(2)),
                sheetId);
    }

    public boolean isMember(String sheetId, String pool, long personId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pool_member WHERE sheet_id = ? AND pool_name = ? AND person_id = ?", Integer.class,
                sheetId, pool, personId);
        return n != null && n > 0;
    }

    public void addMember(PoolMember member) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, share, listed) VALUES (?, ?, ?, ?, "
                + "(SELECT COALESCE(MAX(listed), 0) + 1 FROM pool_member WHERE sheet_id = ?))",
                member.sheetId(), member.pool(), member.personId(), member.share(), member.sheetId());
    }
}
