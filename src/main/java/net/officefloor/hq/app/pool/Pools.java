package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The pools of each sheet, in the order added, and each pool's members in the order added. */
@Repository
public class Pools {

    private static final BigDecimal FULL = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    public Pools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every pool on the sheet with its members, its total share and whether it is balanced. A retired
     *  member (a retirement date set, whatever the date) counts 0.00 toward the total. */
    public List<PoolResponse> onSheet(String sheetId) {
        Map<String, List<PoolMemberResponse>> members = new LinkedHashMap<>();
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        jdbc.query("SELECT name FROM pool WHERE sheet_id = ? ORDER BY position", rs -> {
            members.put(rs.getString(1), new ArrayList<>());
            totals.put(rs.getString(1), BigDecimal.ZERO.setScale(2));
        }, sheetId);
        jdbc.query("SELECT m.pool, m.person_id, p.name, m.share, p.retired_from IS NOT NULL FROM pool_member m "
                + "JOIN person p ON p.id = m.person_id WHERE m.sheet_id = ? ORDER BY m.position", rs -> {
                    String pool = rs.getString(1);
                    List<PoolMemberResponse> list = members.get(pool);
                    if (list == null) return;
                    BigDecimal share = rs.getBigDecimal(4).setScale(2);
                    boolean retired = rs.getBoolean(5);
                    list.add(new PoolMemberResponse(rs.getLong(2), rs.getString(3), share.toPlainString(), retired));
                    if (!retired) totals.merge(pool, share, BigDecimal::add);
                }, sheetId);
        List<PoolResponse> pools = new ArrayList<>();
        members.forEach((name, list) -> {
            BigDecimal total = totals.get(name);
            pools.add(new PoolResponse(name, list, total.toPlainString(), total.compareTo(FULL) == 0));
        });
        return pools;
    }

    /** Whether the sheet has a pool of that name. */
    public boolean has(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether the person is a member of the pool. */
    public boolean hasMember(String sheetId, String pool, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE sheet_id = ? AND pool = ? AND person_id = ?",
                Integer.class, sheetId, pool, personId);
        return n != null && n > 0;
    }

    public void add(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name, position) VALUES (?, ?, "
                + "(SELECT COALESCE(MAX(position), 0) + 1 FROM pool WHERE sheet_id = ?))",
                sheetId, name, sheetId);
    }

    public void addMember(String sheetId, String pool, long personId, BigDecimal share) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool, person_id, share, position) VALUES (?, ?, ?, ?, "
                + "(SELECT COALESCE(MAX(position), 0) + 1 FROM pool_member WHERE sheet_id = ?))",
                sheetId, pool, personId, share, sheetId);
    }

    /** The name of the sheet's person with that id, or null when the sheet has none. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.query("SELECT name FROM person WHERE sheet_id = ? AND id = ?",
                (rs, i) -> rs.getString(1), sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }
}
