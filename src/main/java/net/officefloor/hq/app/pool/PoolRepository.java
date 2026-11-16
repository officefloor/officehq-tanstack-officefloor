package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored pools and their members, kept per sheet. */
@Repository
public class PoolRepository {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final JdbcTemplate jdbc;

    private final SettingRepository settings;

    private final SheetRepository sheets;

    public PoolRepository(JdbcTemplate jdbc, SettingRepository settings, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.sheets = sheets;
    }

    /** The sheet's pools in the order added, each with its members in the order added. A retired member's share (one
     *  with a retirement date, whatever the date) counts as given toward the total, though they get no cut (see cuts);
     *  balanced is a total of exactly 100.00.
     *  Each member of a balanced pool has their cut of the pool's pot (see cuts): a pool that is a member of none has the
     *  sheet's pot, a member pool its cut in the pool above (none when that pool has no cuts); with no pot, no one has.
     *  A person with a hold in force on the sheet's today gets no cut (their share still counts toward the total). A
     *  member whose membership dates do not include the sheet's today is left out entirely: not in the total, no cut. */
    public List<Pool> list(String sheetId) {
        return list(sheetId, null);
    }

    /** The sheet's pools (see #list) read as of a date (null: now): the date stands in for the sheet's today, and the pot is
     *  the one stored by then. */
    public List<Pool> list(String sheetId, LocalDate asOf) {
        BigDecimal pot = asOf != null ? settings.pot(sheetId, asOf) : settings.pot(sheetId);
        LocalDate today = asOf != null ? asOf : sheets.today(sheetId);
        Set<Long> held = held(sheetId, today);
        Map<Long, String> names = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    names.put(rs.getLong("id"), rs.getString("name"));
                }, sheetId);
        Map<Long, List<PoolMember>> members = new LinkedHashMap<>();
        Map<Long, Long> parents = new HashMap<>();
        jdbc.query("SELECT m.pool_id, m.person_id, m.member_pool_id, m.share, m.from_date, m.to_date, p.name, p.retired_from,"
                + " q.name AS pool_name"
                + " FROM pool_member m JOIN pool o ON o.id = m.pool_id LEFT JOIN person p ON p.id = m.person_id"
                + " LEFT JOIN pool q ON q.id = m.member_pool_id WHERE o.sheet_id = ? ORDER BY m.id",
                rs -> {
                    long poolId = rs.getLong("pool_id");
                    List<PoolMember> in = members.computeIfAbsent(poolId, k -> new ArrayList<>());
                    BigDecimal share = rs.getBigDecimal("share").setScale(2);
                    java.sql.Date fromDate = rs.getDate("from_date");
                    java.sql.Date toDate = rs.getDate("to_date");
                    LocalDate from = fromDate == null ? null : fromDate.toLocalDate();
                    LocalDate to = toDate == null ? null : toDate.toLocalDate();
                    String fromText = from == null ? null : from.toString();
                    String toText = to == null ? null : to.toString();
                    boolean active = (from == null || !today.isBefore(from)) && (to == null || !today.isAfter(to));
                    long memberPool = rs.getLong("member_pool_id");
                    if (rs.wasNull()) {
                        in.add(new PoolMember(rs.getLong("person_id"), rs.getString("name"), null, share,
                                rs.getDate("retired_from") != null, null, fromText, toText, active));
                    } else {
                        String name = rs.getString("pool_name");
                        in.add(new PoolMember(null, name, name, share, false, null, fromText, toText, active));
                        parents.put(memberPool, poolId);
                    }
                }, sheetId);
        Map<Long, Pool> pools = new HashMap<>();
        List<Pool> inOrder = new ArrayList<>();
        for (Long id : names.keySet()) {
            // a pool above is built first; the list keeps the order added
            inOrder.add(share(id, names, members, parents, pot, held, pools));
        }
        inOrder.addAll(crews(sheetId, pot, held));
        return inOrder;
    }

    /** The sheet's crews as pools, after the stored pools, in the order added: each under its name, with the lead and then
     *  the members (in the order added) at equal shares of 100.00 (in cents, the remainder on the lead), so always
     *  balanced, and cut from the sheet's pot like any pool that is a member of none. */
    private List<Pool> crews(String sheetId, BigDecimal pot, Set<Long> held) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, List<Object[]>> people = new LinkedHashMap<>();
        jdbc.query("SELECT c.id, c.name, c.lead_id, p.name AS lead, p.retired_from FROM crew c"
                + " JOIN person p ON p.id = c.lead_id WHERE c.sheet_id = ? ORDER BY c.id",
                rs -> {
                    long id = rs.getLong("id");
                    names.put(id, rs.getString("name"));
                    people.computeIfAbsent(id, k -> new ArrayList<>()).add(new Object[] { rs.getLong("lead_id"),
                            rs.getString("lead"), rs.getDate("retired_from") != null });
                }, sheetId);
        jdbc.query("SELECT m.crew_id, m.person_id, p.name, p.retired_from FROM crew_member m JOIN crew c ON c.id = m.crew_id"
                + " JOIN person p ON p.id = m.person_id WHERE c.sheet_id = ? ORDER BY m.id",
                rs -> {
                    List<Object[]> in = people.get(rs.getLong("crew_id"));
                    long personId = rs.getLong("person_id");
                    if (in != null && in.stream().noneMatch(m -> m[0].equals(personId))) {
                        in.add(new Object[] { personId, rs.getString("name"), rs.getDate("retired_from") != null });
                    }
                }, sheetId);
        List<Pool> out = new ArrayList<>();
        for (Map.Entry<Long, String> crew : names.entrySet()) {
            List<Object[]> in = people.get(crew.getKey());
            long each = 10000 / in.size();
            long remainder = 10000 - each * in.size();
            List<PoolMember> members = new ArrayList<>();
            for (int i = 0; i < in.size(); i++) {
                Object[] m = in.get(i);
                BigDecimal share = BigDecimal.valueOf(i == 0 ? each + remainder : each, 2);
                members.add(new PoolMember((Long) m[0], (String) m[1], null, share, (Boolean) m[2], null, null, null,
                        true));
            }
            out.add(new Pool(crew.getValue(), pot != null ? cuts(members, pot, held) : members, HUNDRED, true));
        }
        return out;
    }

    /** Whether the sheet has a crew by that name (a pool cannot take a crew's name). */
    public boolean isCrew(String sheetId, String name) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM crew WHERE sheet_id = ? AND name = ?", Integer.class, sheetId,
                name) > 0;
    }

    /** The ids of the sheet's people with a hold whose dates include the day (the sheet's today). */
    private Set<Long> held(String sheetId, LocalDate on) {
        java.sql.Date today = java.sql.Date.valueOf(on);
        return Set.copyOf(jdbc.queryForList("SELECT DISTINCT person_id FROM person_hold WHERE sheet_id = ?"
                + " AND from_date <= ? AND (to_date IS NULL OR to_date >= ?)", Long.class, sheetId, today, today));
    }

    /** Builds the pool (and, first, the pool above it), its members cut from its pot when it is balanced. */
    private Pool share(Long id, Map<Long, String> names, Map<Long, List<PoolMember>> members, Map<Long, Long> parents,
            BigDecimal sheetPot, Set<Long> held, Map<Long, Pool> pools) {
        Pool done = pools.get(id);
        if (done != null) {
            return done;
        }
        BigDecimal pot = sheetPot;
        Long parent = parents.get(id);
        if (parent != null) {
            pot = null;
            Pool above = share(parent, names, members, parents, sheetPot, held, pools);
            for (PoolMember m : above.members()) {
                if (names.get(id).equals(m.pool()) && m.cut() != null) {
                    pot = new BigDecimal(m.cut());
                }
            }
        }
        List<PoolMember> in = members.getOrDefault(id, List.of());
        BigDecimal total = new BigDecimal("0.00");
        for (PoolMember m : in) {
            if (m.active()) {
                total = total.add(m.share());
            }
        }
        boolean balanced = total.compareTo(HUNDRED) == 0;
        Pool pool = new Pool(names.get(id), balanced && pot != null ? cuts(in, pot, held) : in, total, balanced);
        pools.put(id, pool);
        return pool;
    }

    /** The members with their cuts of the pot. When every active member is retired, no member has a cut (each null).
     *  Otherwise a retired member, and a person held on the sheet's today, gets 0.00; each
     *  other member the pot times their share divided by the shares of the members with a cut added up, rounded half to
     *  even to two decimals; the pot less the cuts added up goes to the member with the smallest non-zero cut, the
     *  latest added on a tie (a member with no cut gets none; when no cut is non-zero, it goes to the last member with a
     *  cut and a share), so the cuts total exactly the pot (when no member has a cut, each has 0.00). */
    static List<PoolMember> cuts(List<PoolMember> in, BigDecimal pot, Set<Long> held) {
        boolean allRetired = true;
        for (PoolMember m : in) {
            if (m.active() && !m.retired()) {
                allRetired = false;
            }
        }
        if (allRetired) {
            return in;
        }
        BigDecimal shares = new BigDecimal("0.00");
        for (PoolMember m : in) {
            if (hasCut(m, held)) {
                shares = shares.add(m.share());
            }
        }
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = new BigDecimal("0.00");
        int smallest = -1;
        int lastShared = -1;
        for (int i = 0; i < in.size(); i++) {
            PoolMember m = in.get(i);
            boolean cut = hasCut(m, held) && shares.signum() > 0;
            BigDecimal value = cut ? pot.multiply(m.share()).divide(shares, 2, RoundingMode.HALF_EVEN)
                    : new BigDecimal("0.00");
            cuts.add(value);
            sum = sum.add(value);
            if (cut && value.signum() != 0 && (smallest < 0 || value.compareTo(cuts.get(smallest)) <= 0)) {
                smallest = i;
            }
            if (cut && m.share().signum() > 0) {
                lastShared = i;
            }
        }
        int remainder = smallest >= 0 ? smallest : lastShared;
        if (remainder >= 0) {
            cuts.set(remainder, cuts.get(remainder).add(pot.subtract(sum)));
        }
        List<PoolMember> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            out.add(in.get(i).withCut(cuts.get(i)));
        }
        return out;
    }

    /** Whether the member takes a cut: active on the sheet's today, not retired, and not a person held on it. */
    private static boolean hasCut(PoolMember m, Set<Long> held) {
        return m.active() && !m.retired() && (m.personId() == null || !held.contains(m.personId()));
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

    /** The id of the pool the pool is a member of, or null when it is a member of none. */
    public Long parentOf(long poolId) {
        List<Long> ids = jdbc.queryForList("SELECT pool_id FROM pool_member WHERE member_pool_id = ?", Long.class,
                poolId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** How many pools deep the chain from the pool down is: 1 for a pool with no member pools. */
    public int height(long poolId) {
        int deepest = 0;
        for (Long member : jdbc.queryForList(
                "SELECT member_pool_id FROM pool_member WHERE pool_id = ? AND member_pool_id IS NOT NULL", Long.class,
                poolId)) {
            deepest = Math.max(deepest, height(member));
        }
        return deepest + 1;
    }

    /** Adds the member pool to the pool (it is checked to be a member of none). */
    public void addPoolMember(long poolId, long memberPoolId, BigDecimal share, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO pool_member (pool_id, member_pool_id, share, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                poolId, memberPoolId, share, sqlDate(from), sqlDate(to));
    }

    /** The name of the person on the sheet, or null when there is no such person there. */
    public String personName(String sheetId, long personId) {
        List<String> names = jdbc.queryForList("SELECT name FROM person WHERE sheet_id = ? AND id = ?", String.class,
                sheetId, personId);
        return names.isEmpty() ? null : names.get(0);
    }

    /** Adds the person to the pool; false when they are already in it. */
    public boolean addMember(long poolId, long personId, BigDecimal share, LocalDate from, LocalDate to) {
        try {
            jdbc.update("INSERT INTO pool_member (pool_id, person_id, share, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                    poolId, personId, share, sqlDate(from), sqlDate(to));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private static java.sql.Date sqlDate(LocalDate date) {
        return date == null ? null : java.sql.Date.valueOf(date);
    }
}
