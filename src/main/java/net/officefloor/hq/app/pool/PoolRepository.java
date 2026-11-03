package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** The sheet's pot; null when there is none. */
    private BigDecimal pot(String sheetId) {
        List<BigDecimal> found = jdbc.query("SELECT pot FROM sheet WHERE id = ?", (rs, i) -> rs.getBigDecimal("pot"),
                sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Each member's cut of the pot: the pot times their share divided by 100, rounded half to even to two
     *  decimals (0.00 for a retired member); the remainder, the pot minus the cuts added up, goes to the member
     *  with the largest share (the earliest added on a tie), so the cuts total exactly the pot. */
    private static List<PoolMember> cut(List<PoolMember> members, BigDecimal pot) {
        List<BigDecimal> cuts = new ArrayList<>();
        int largest = -1;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            BigDecimal share = m.retired() ? BigDecimal.ZERO : new BigDecimal(m.share());
            BigDecimal cut = pot.multiply(share).divide(HUNDRED, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!m.retired() && (largest < 0 || share.compareTo(new BigDecimal(members.get(largest).share())) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        List<PoolMember> cut = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            cut.add(new PoolMember(m.personId(), m.name(), m.share(), m.retired(), cuts.get(i).setScale(2).toPlainString()));
        }
        return cut;
    }

    /** The sheet's pools, by name, each with its members in the order they were added, and their cuts of the
     *  sheet's pot when the pool is balanced and there is a pot. */
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
                        rs.getBigDecimal("share").setScale(2).toPlainString(), rs.getDate("retired_from") != null, null));
            }
        }, sheetId);
        BigDecimal pot = pot(sheetId);
        List<Pool> pools = new ArrayList<>();
        members.forEach((name, of) -> {
            BigDecimal total = of.stream().filter(m -> !m.retired()).map(m -> new BigDecimal(m.share()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
            boolean balanced = total.compareTo(HUNDRED) == 0;
            pools.add(new Pool(name, balanced && pot != null ? cut(of, pot) : of, total.toPlainString(), balanced));
        });
        return pools;
    }
}
