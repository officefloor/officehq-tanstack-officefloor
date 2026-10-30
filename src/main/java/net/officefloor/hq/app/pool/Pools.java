package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The pool store: each sheet's pools, in the order added, and their members. */
@Repository
public class Pools {

    /** The shares of a balanced pool total exactly this. */
    public static final BigDecimal FULL = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    public Pools(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's pools, in the order added, each with its members in the order added. A retired member
     *  (a retirement date set, whatever the date) counts 0.00 toward the total. */
    public List<PoolResponse> list(String sheetId) {
        Map<String, List<PoolResponse.Member>> members = new LinkedHashMap<>();
        for (String name : jdbc.queryForList("SELECT name FROM pool WHERE sheet_id = ? ORDER BY seq", String.class, sheetId)) {
            members.put(name, new ArrayList<>());
        }
        jdbc.query("SELECT m.pool_name, m.person_id, p.name, m.share, p.retired_from FROM pool_member m"
                + " JOIN person p ON p.id = m.person_id AND p.sheet_id = m.sheet_id"
                + " WHERE m.sheet_id = ? ORDER BY m.seq", rs -> {
                    List<PoolResponse.Member> list = members.get(rs.getString(1));
                    if (list != null) {
                        list.add(new PoolResponse.Member(String.valueOf(rs.getLong(2)), rs.getString(3),
                                rs.getBigDecimal(4).setScale(2).toPlainString(), rs.getDate(5) != null));
                    }
                }, sheetId);
        List<PoolResponse> pools = new ArrayList<>();
        members.forEach((name, list) -> {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolResponse.Member m : list) {
                if (!m.retired()) {
                    total = total.add(new BigDecimal(m.share()));
                }
            }
            pools.add(new PoolResponse(name, total.toPlainString(), total.compareTo(FULL) == 0, list));
        });
        return pools;
    }

    /** Whether the sheet has a pool of the name. */
    public boolean contains(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool WHERE sheet_id = ? AND name = ?", Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether the person is a member of the pool. */
    public boolean hasMember(String sheetId, String pool, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pool_member WHERE sheet_id = ? AND pool_name = ? AND person_id = ?",
                Integer.class, sheetId, pool, personId);
        return n != null && n > 0;
    }

    /** Adds a pool to the sheet. */
    public void add(String sheetId, String name) {
        jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
    }

    /** Adds the person to the pool with the share. */
    public void addMember(String sheetId, String pool, long personId, BigDecimal share) {
        jdbc.update("INSERT INTO pool_member (sheet_id, pool_name, person_id, share) VALUES (?, ?, ?, ?)",
                sheetId, pool, personId, share);
    }
}
