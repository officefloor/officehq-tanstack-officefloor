package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The pools on each sheet and their members. */
@Repository
public class Pools {

    private static final BigDecimal FULL = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    public Pools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every pool on the sheet in the order added, its members in the order added. */
    public List<Pool> list(String sheet) {
        Map<String, List<PoolMember>> members = new LinkedHashMap<>();
        jdbc.query("SELECT name FROM pool WHERE sheet = ? ORDER BY position",
                (rs) -> {
                    members.put(rs.getString("name"), new ArrayList<>());
                }, sheet);
        jdbc.query("SELECT m.pool, m.person_id, p.name, m.share, p.retired_from FROM pool_member m"
                + " JOIN person p ON p.sheet = m.sheet AND p.id = m.person_id WHERE m.sheet = ? ORDER BY m.position",
                (rs) -> {
                    List<PoolMember> list = members.get(rs.getString("pool"));
                    if (list != null) {
                        list.add(new PoolMember(rs.getLong("person_id"), rs.getString("name"),
                                rs.getBigDecimal("share").setScale(2).toPlainString(),
                                rs.getDate("retired_from") != null));
                    }
                }, sheet);
        List<Pool> pools = new ArrayList<>();
        members.forEach((name, list) -> {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolMember m : list) {
                if (!m.retired()) {
                    total = total.add(new BigDecimal(m.share()));
                }
            }
            pools.add(new Pool(name, list, total.toPlainString(), total.compareTo(FULL) == 0));
        });
        return pools;
    }

    /** Whether the sheet has a pool of this name. */
    public boolean has(String sheet, String name) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet = ? AND name = ?", Integer.class,
                sheet, name);
        return found != null && found > 0;
    }

    /** Whether the person is in the pool. */
    public boolean isMember(String sheet, String pool, long person) {
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pool_member WHERE sheet = ? AND pool = ? AND person_id = ?", Integer.class, sheet,
                pool, person);
        return found != null && found > 0;
    }

    public void add(String sheet, String name) {
        jdbc.update("INSERT INTO pool (sheet, name, position) VALUES (?, ?,"
                + " (SELECT COALESCE(MAX(p.position), 0) + 1 FROM pool p WHERE p.sheet = ?))", sheet, name, sheet);
    }

    public void addMember(String sheet, String pool, long person, BigDecimal share) {
        jdbc.update("INSERT INTO pool_member (sheet, pool, person_id, share, position) VALUES (?, ?, ?, ?,"
                + " (SELECT COALESCE(MAX(m.position), 0) + 1 FROM pool_member m WHERE m.sheet = ?))", sheet, pool,
                person, share, sheet);
    }
}
