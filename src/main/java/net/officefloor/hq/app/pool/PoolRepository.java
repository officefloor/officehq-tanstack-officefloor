package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PoolRepository {

    private static final BigDecimal FULL = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    public PoolRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's pools in the order they were added, each with its members in the order they were added. */
    public List<Pool> list(String sheetId) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, List<PoolMember>> members = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? ORDER BY id", rs -> {
            names.put(rs.getLong("id"), rs.getString("name"));
            members.put(rs.getLong("id"), new ArrayList<>());
        }, sheetId);
        jdbc.query("SELECT m.pool_id, m.person_id, p.name, m.share, p.retired_from FROM pool_member m "
                + "JOIN person p ON p.id = m.person_id WHERE m.sheet_id = ? ORDER BY m.id", rs -> {
                    List<PoolMember> of = members.get(rs.getLong("pool_id"));
                    if (of != null) {
                        of.add(new PoolMember(rs.getLong("person_id"), rs.getString("name"),
                                rs.getBigDecimal("share").setScale(2).toPlainString(),
                                rs.getObject("retired_from") != null));
                    }
                }, sheetId);
        List<Pool> pools = new ArrayList<>();
        for (Map.Entry<Long, String> pool : names.entrySet()) {
            List<PoolMember> of = members.get(pool.getKey());
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolMember m : of) {
                if (!m.retired()) total = total.add(new BigDecimal(m.share()));
            }
            pools.add(new Pool(pool.getKey(), sheetId, pool.getValue(), of, total.toPlainString(),
                    total.compareTo(FULL) == 0));
        }
        return pools;
    }

    public boolean contains(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** The pool's name, or null when there is no such pool on the sheet. */
    public String nameOf(String sheetId, long poolId) {
        List<String> found = jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ? AND id = ?",
                String.class, sheetId, poolId);
        return found.isEmpty() ? null : found.get(0);
    }

    public boolean hasMember(long poolId, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE pool_id = ? AND person_id = ?",
                Integer.class, poolId, personId);
        return n != null && n > 0;
    }

    public void add(String sheetId, String name, String by) {
        jdbc.update("INSERT INTO pool (sheet_id, name, added_by) VALUES (?, ?, ?)", sheetId, name, by);
    }

    public void addMember(String sheetId, long poolId, long personId, BigDecimal share, String by) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_id, person_id, share, added_by) VALUES (?, ?, ?, ?, ?)",
                sheetId, poolId, personId, share, by);
    }
}
