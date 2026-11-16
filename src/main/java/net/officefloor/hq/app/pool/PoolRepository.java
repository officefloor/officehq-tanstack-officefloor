package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored pools and their members, kept per sheet. */
@Repository
public class PoolRepository {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    private final SettingRepository settings;

    public PoolRepository(JdbcTemplate jdbc, SettingRepository settings) {
        this.jdbc = jdbc;
        this.settings = settings;
    }

    /** The sheet's pools in the order added, each with its members in the order added. A retired member's share (one
     *  with a retirement date, whatever the date) counts as 0.00 toward the total; balanced is a total of exactly 100.00.
     *  Each member of a balanced pool has their cut of the sheet's pot (see cuts); with no pot, no one has. */
    public List<Pool> list(String sheetId) {
        BigDecimal pot = settings.pot(sheetId);
        Map<Long, String> names = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    names.put(rs.getLong("id"), rs.getString("name"));
                }, sheetId);
        Map<Long, List<PoolMember>> members = new LinkedHashMap<>();
        jdbc.query("SELECT m.pool_id, m.person_id, m.share, p.name, p.retired_from FROM pool_member m"
                + " JOIN pool o ON o.id = m.pool_id JOIN person p ON p.id = m.person_id"
                + " WHERE o.sheet_id = ? ORDER BY m.id",
                rs -> {
                    members.computeIfAbsent(rs.getLong("pool_id"), k -> new ArrayList<>())
                            .add(new PoolMember(rs.getLong("person_id"), rs.getString("name"),
                                    rs.getBigDecimal("share").setScale(2), rs.getDate("retired_from") != null, null));
                }, sheetId);
        List<Pool> pools = new ArrayList<>();
        for (Map.Entry<Long, String> e : names.entrySet()) {
            List<PoolMember> in = members.getOrDefault(e.getKey(), List.of());
            BigDecimal total = new BigDecimal("0.00");
            for (PoolMember m : in) {
                if (!m.retired()) {
                    total = total.add(m.share());
                }
            }
            boolean balanced = total.compareTo(HUNDRED) == 0;
            pools.add(new Pool(e.getValue(), balanced && pot != null ? cuts(in, pot) : in, total, balanced));
        }
        return pools;
    }

    /** The members with their cuts of the pot: the pot times their share divided by 100, rounded half to even to two
     *  decimals (a retired member's 0.00); the pot less the cuts added up goes to the member with the largest share (not
     *  retired), the earliest added on a tie, so the cuts total exactly the pot. */
    static List<PoolMember> cuts(List<PoolMember> in, BigDecimal pot) {
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = new BigDecimal("0.00");
        int largest = -1;
        for (int i = 0; i < in.size(); i++) {
            PoolMember m = in.get(i);
            BigDecimal cut = m.retired() ? new BigDecimal("0.00")
                    : pot.multiply(m.share()).divide(HUNDRED).setScale(2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!m.retired() && (largest < 0 || m.share().compareTo(in.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        }
        List<PoolMember> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            out.add(in.get(i).withCut(cuts.get(i)));
        }
        return out;
    }

    /** Adds the pool; false when the name is already taken on the sheet (the unique key refuses a second). */
    public boolean add(String sheetId, String name) {
        try {
            jdbc.update("INSERT INTO pool (sheet_id, name) VALUES (?, ?)", sheetId, name);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** The id of the sheet's pool by that name, or null when there is none. */
    public Long idOf(String sheetId, String name) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM pool WHERE sheet_id = ? AND name = ?", Long.class, sheetId,
                name);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** The name of the person on the sheet, or null when there is no such person there. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.queryForList("SELECT name FROM person WHERE sheet_id = ? AND id = ?", String.class,
                sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }

    /** Adds the person to the pool; false when they are already in it. */
    public boolean addMember(long poolId, long personId, BigDecimal share) {
        try {
            jdbc.update("INSERT INTO pool_member (pool_id, person_id, share) VALUES (?, ?, ?)", poolId, personId, share);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
