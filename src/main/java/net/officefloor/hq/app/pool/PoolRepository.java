package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PoolRepository {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    public PoolRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    public boolean exists(String sheetId, String name) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, name);
        return known != null && known > 0;
    }

    /** The person's name, or null when they are not on the sheet. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.queryForList("SELECT name FROM person WHERE sheet_id = ? AND id = ?",
                String.class, sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }

    /** Add a pool; false when the sheet already has one of that name. */
    public boolean add(String sheetId, String name) {
        try {
            jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Add a member to a pool; false when the person is already in it. */
    public boolean addMember(String sheetId, String pool, long personId, BigDecimal share) {
        try {
            jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, share) VALUES (?, ?, ?, ?)",
                    sheetId, pool, personId, share);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** The sheet's pools, by name, each with its members in the order they were added. */
    public List<Pool> list(String sheetId) {
        Map<String, List<PoolMember>> members = new LinkedHashMap<>();
        for (String name : jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ? ORDER BY name", String.class,
                sheetId)) {
            members.put(name, new ArrayList<>());
        }
        jdbc.query("SELECT m.pool_name, m.person_id, p.name, m.share, p.retired_from FROM pool_member m"
                + " JOIN person p ON p.id = m.person_id WHERE m.sheet_id = ? ORDER BY m.id", rs -> {
            List<PoolMember> of = members.get(rs.getString("pool_name"));
            if (of != null) {
                of.add(new PoolMember(rs.getLong("person_id"), rs.getString("name"),
                        rs.getBigDecimal("share").setScale(2).toPlainString(), rs.getDate("retired_from") != null));
            }
        }, sheetId);
        List<Pool> pools = new ArrayList<>();
        members.forEach((name, of) -> {
            BigDecimal total = of.stream().filter(m -> !m.retired()).map(m -> new BigDecimal(m.share()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
            pools.add(new Pool(name, of, total.toPlainString(), total.compareTo(HUNDRED) == 0));
        });
        return pools;
    }
}
