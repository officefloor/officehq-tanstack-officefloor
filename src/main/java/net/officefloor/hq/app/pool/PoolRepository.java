package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PoolRepository {

    private static final BigDecimal FULL = new BigDecimal("100.00");

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final JdbcTemplate jdbc;
    private final SettingRepository settings;

    public PoolRepository(JdbcTemplate jdbc, SettingRepository settings) {
        this.jdbc = jdbc;
        this.settings = settings;
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
                                rs.getObject("retired_from") != null, ""));
                    }
                }, sheetId);
        BigDecimal pot = settings.pot(sheetId);
        List<Pool> pools = new ArrayList<>();
        for (Map.Entry<Long, String> pool : names.entrySet()) {
            List<PoolMember> of = members.get(pool.getKey());
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (PoolMember m : of) {
                if (!m.retired()) total = total.add(new BigDecimal(m.share()));
            }
            boolean balanced = total.compareTo(FULL) == 0;
            pools.add(new Pool(pool.getKey(), sheetId, pool.getValue(),
                    balanced && pot != null ? cut(of, pot) : of, total.toPlainString(), balanced));
        }
        return pools;
    }

    /** The members with their cuts of the pot: the pot times their share over 100, rounded half to even
     *  to two decimals (a retired member's 0.00); the remainder goes to the member with the largest share
     *  (the earliest added on a tie), so the cuts total exactly the pot. */
    private static List<PoolMember> cut(List<PoolMember> members, BigDecimal pot) {
        BigDecimal[] cuts = new BigDecimal[members.size()];
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            BigDecimal share = new BigDecimal(m.share());
            cuts[i] = m.retired() ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(share).divide(HUNDRED).setScale(2, RoundingMode.HALF_EVEN);
            sum = sum.add(cuts[i]);
            if (!m.retired() && (largest < 0 || share.compareTo(new BigDecimal(members.get(largest).share())) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) cuts[largest] = cuts[largest].add(pot.subtract(sum));
        List<PoolMember> cut = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) cut.add(members.get(i).withCut(cuts[i].toPlainString()));
        return cut;
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

    /** Move the person {@code fromId}'s pool memberships to {@code intoId}: their share added to
     *  into's where into is already a member. */
    public void moveMembers(String sheetId, long fromId, long intoId) {
        jdbc.update("UPDATE pool_member m SET share = share + (SELECT f.share FROM pool_member f "
                + "WHERE f.pool_id = m.pool_id AND f.person_id = ?) WHERE m.sheet_id = ? AND m.person_id = ? "
                + "AND m.pool_id IN (SELECT f.pool_id FROM pool_member f WHERE f.person_id = ?)",
                fromId, sheetId, intoId, fromId);
        jdbc.update("DELETE FROM pool_member WHERE sheet_id = ? AND person_id = ? AND pool_id IN "
                + "(SELECT m.pool_id FROM pool_member m WHERE m.person_id = ?)", sheetId, fromId, intoId);
        jdbc.update("UPDATE pool_member SET person_id = ? WHERE sheet_id = ? AND person_id = ?", intoId, sheetId, fromId);
    }
}
