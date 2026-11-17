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
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
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

    private final PeriodRepository periods;

    public PoolRepository(JdbcTemplate jdbc, SettingRepository settings, SheetRepository sheets,
            PeriodRepository periods) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.sheets = sheets;
        this.periods = periods;
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
     *  the one stored by then. Each member carries their cut history, by the periods' from-dates: for every period closed
     *  or soft-closed by then, their cut fixed when it was (see #liveCuts), whatever its dates; for every other period
     *  whose to-date is on or before the sheet's today, their cut as it stood at the end of that period (see #stood).
     *  Null when they had none then. */
    public List<Pool> list(String sheetId, LocalDate asOf) {
        return list(sheetId, asOf, null);
    }

    /** The sheet's pools (see #list(String, LocalDate)), with the preview date (null: none; see Preview) standing in for
     *  the sheet's today when read as it is now. */
    public List<Pool> list(String sheetId, LocalDate asOf, LocalDate preview) {
        LocalDate shown = asOf == null ? preview : null;
        List<Pool> now = stood(sheetId, asOf, shown);
        LocalDate today = asOf != null ? asOf : shown != null ? shown : sheets.today(sheetId);
        Map<Long, String> fixed = periods.poolCuts(sheetId, asOf);
        List<String> ended = new ArrayList<>();
        List<Map<String, String>> cutsThen = new ArrayList<>();
        for (Period period : periods.list(sheetId)) {
            String text = fixed.get(period.id());
            if (text != null) {
                ended.add(period.name());
                cutsThen.add(parseCuts(text));
            } else if (!period.to().isAfter(today)) {
                ended.add(period.name());
                cutsThen.add(cutsOf(stood(sheetId, period.to())));
            }
        }
        List<Pool> out = new ArrayList<>();
        for (Pool pool : now) {
            List<PoolMember> members = new ArrayList<>();
            for (PoolMember m : pool.members()) {
                List<PeriodCut> history = new ArrayList<>();
                for (int i = 0; i < ended.size(); i++) {
                    history.add(new PeriodCut(ended.get(i), cutsThen.get(i).get(key(pool, m))));
                }
                members.add(m.withHistory(history));
            }
            out.add(new Pool(pool.name(), members, pool.totalShare(), pool.balanced()));
        }
        return out;
    }

    /** Each person's cut total as of a date (null: now): their cuts across every pool they are a member of (see #list),
     *  added up, by person id; a person with no cut is left out. */
    public Map<Long, BigDecimal> cutTotals(String sheetId, LocalDate asOf) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (Pool pool : stood(sheetId, asOf)) {
            for (PoolMember m : pool.members()) {
                if (m.pool() == null && m.personId() != null && m.cut() != null) {
                    totals.merge(m.personId(), new BigDecimal(m.cut()), BigDecimal::add);
                }
            }
        }
        return totals;
    }

    /** Every member's cut as it stands now, as text to fix on a period's close: one line per member, its key (see #key),
     *  a tab and the cut (empty when none), joined by newlines. */
    public String liveCuts(String sheetId) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> e : cutsOf(stood(sheetId, null)).entrySet()) {
            text.append(text.length() == 0 ? "" : "\n").append(e.getKey()).append('\t')
                    .append(e.getValue() == null ? "" : e.getValue());
        }
        return text.toString();
    }

    /** The cuts by member key from their text (see #liveCuts). */
    private static Map<String, String> parseCuts(String text) {
        Map<String, String> cuts = new HashMap<>();
        for (String line : text.split("\n")) {
            int at = line.lastIndexOf('\t');
            if (at > 0) {
                String cut = line.substring(at + 1);
                cuts.put(line.substring(0, at), cut.isEmpty() ? null : cut);
            }
        }
        return cuts;
    }

    /** Each member's cut in the pools, by member key. */
    private static Map<String, String> cutsOf(List<Pool> pools) {
        Map<String, String> cuts = new LinkedHashMap<>();
        for (Pool pool : pools) {
            for (PoolMember m : pool.members()) {
                cuts.put(key(pool, m), m.cut());
            }
        }
        return cuts;
    }

    /** The member of the pool, the same across reads. */
    private static String key(Pool pool, PoolMember m) {
        return pool.name() + "\t" + (m.pool() != null ? "pool:" + m.pool() : "person:" + m.personId());
    }

    /** The sheet's pools as they stood at the end of the date (null: now), without cut histories: the date stands in for
     *  the sheet's today, and only the pools, memberships, crews, holds, retirements and pot stored while the sheet's
     *  today was on or before it count. */
    private List<Pool> stood(String sheetId, LocalDate asOf) {
        return stood(sheetId, asOf, null);
    }

    /** The pools as they stood (see #stood(String, LocalDate)), the preview date (null: none) standing in for the
     *  sheet's today when read as it is now. */
    private List<Pool> stood(String sheetId, LocalDate asOf, LocalDate preview) {
        BigDecimal pot = asOf != null ? settings.pot(sheetId, asOf) : settings.pot(sheetId);
        LocalDate today = asOf != null ? asOf : preview != null ? preview : sheets.today(sheetId);
        java.sql.Date then = asOf != null ? java.sql.Date.valueOf(asOf) : java.sql.Date.valueOf(LocalDate.of(9999, 12, 31));
        Set<Long> held = held(sheetId, today, then);
        Map<Long, Boolean> retiredThen = asOf != null ? retired(sheetId, then) : null;
        Map<Long, String> names = new LinkedHashMap<>();
        jdbc.query("SELECT id, name FROM pool WHERE sheet_id = ? AND (made_on IS NULL OR made_on <= ?) ORDER BY id",
                rs -> {
                    names.put(rs.getLong("id"), rs.getString("name"));
                }, sheetId, then);
        Map<Long, List<PoolMember>> members = new LinkedHashMap<>();
        Map<Long, Long> parents = new HashMap<>();
        jdbc.query("SELECT m.pool_id, m.person_id, m.member_pool_id, m.share, m.from_date, m.to_date, p.name, p.retired_from,"
                + " q.name AS pool_name"
                + " FROM pool_member m JOIN pool o ON o.id = m.pool_id LEFT JOIN person p ON p.id = m.person_id"
                + " LEFT JOIN pool q ON q.id = m.member_pool_id WHERE o.sheet_id = ?"
                + " AND (m.made_on IS NULL OR m.made_on <= ?) AND (o.made_on IS NULL OR o.made_on <= ?)"
                + " AND (q.id IS NULL OR q.made_on IS NULL OR q.made_on <= ?) ORDER BY m.id",
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
                        long personId = rs.getLong("person_id");
                        boolean retired = retiredThen == null ? rs.getDate("retired_from") != null
                                : retiredThen.getOrDefault(personId, false);
                        in.add(new PoolMember(personId, rs.getString("name"), null, share, retired, null, fromText,
                                toText, active, List.of()));
                    } else {
                        String name = rs.getString("pool_name");
                        in.add(new PoolMember(null, name, name, share, false, null, fromText, toText, active, List.of()));
                        parents.put(memberPool, poolId);
                    }
                }, sheetId, then, then, then);
        Map<Long, Pool> pools = new HashMap<>();
        List<Pool> inOrder = new ArrayList<>();
        for (Long id : names.keySet()) {
            // a pool above is built first; the list keeps the order added
            inOrder.add(share(id, names, members, parents, pot, held, pools));
        }
        inOrder.addAll(crews(sheetId, pot, held, then, retiredThen));
        return inOrder;
    }

    /** The sheet's crews as pools, after the stored pools, in the order added: each under its name, with the lead and then
     *  the members (in the order added) at equal shares of 100.00 (in cents, the remainder on the lead), so always
     *  balanced, and cut from the sheet's pot like any pool that is a member of none. */
    private List<Pool> crews(String sheetId, BigDecimal pot, Set<Long> held, java.sql.Date then,
            Map<Long, Boolean> retiredThen) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, List<Object[]>> people = new LinkedHashMap<>();
        jdbc.query("SELECT c.id, c.name, c.lead_id, p.name AS lead, p.retired_from FROM crew c"
                + " LEFT JOIN person p ON p.id = c.lead_id WHERE c.sheet_id = ? AND (c.made_on IS NULL OR c.made_on <= ?)"
                + " ORDER BY c.id",
                rs -> {
                    long id = rs.getLong("id");
                    names.put(id, rs.getString("name"));
                    List<Object[]> in = people.computeIfAbsent(id, k -> new ArrayList<>());
                    long leadId = rs.getLong("lead_id");
                    if (!rs.wasNull()) {
                        in.add(new Object[] { leadId, rs.getString("lead"), retiredThen == null
                                ? rs.getDate("retired_from") != null : retiredThen.getOrDefault(leadId, false) });
                    }
                }, sheetId, then);
        // with the memberships a merge removed after the date (see PersonMerger), in the order added
        jdbc.query("SELECT crew_id, person_id, name, retired_from FROM (SELECT m.id, m.crew_id, m.person_id, p.name,"
                + " p.retired_from FROM crew_member m JOIN crew c ON c.id = m.crew_id"
                + " JOIN person p ON p.id = m.person_id WHERE c.sheet_id = ? AND (m.made_on IS NULL OR m.made_on <= ?)"
                + " UNION ALL SELECT l.id, l.crew_id, l.person_id, l.name, CAST(NULL AS DATE) FROM crew_member_left l"
                + " JOIN crew c ON c.id = l.crew_id WHERE c.sheet_id = ? AND (l.made_on IS NULL OR l.made_on <= ?)"
                + " AND l.left_on > ?) x ORDER BY id",
                rs -> {
                    List<Object[]> in = people.get(rs.getLong("crew_id"));
                    long personId = rs.getLong("person_id");
                    if (in != null && in.stream().noneMatch(m -> m[0].equals(personId))) {
                        in.add(new Object[] { personId, rs.getString("name"), retiredThen == null
                                ? rs.getDate("retired_from") != null : retiredThen.getOrDefault(personId, false) });
                    }
                }, sheetId, then, sheetId, then, then);
        List<Pool> out = new ArrayList<>();
        for (Map.Entry<Long, String> crew : names.entrySet()) {
            List<Object[]> in = people.get(crew.getKey());
            long each = in.isEmpty() ? 0 : 10000 / in.size();
            long remainder = 10000 - each * in.size();
            List<PoolMember> members = new ArrayList<>();
            for (int i = 0; i < in.size(); i++) {
                Object[] m = in.get(i);
                BigDecimal share = BigDecimal.valueOf(i == 0 ? each + remainder : each, 2);
                members.add(new PoolMember((Long) m[0], (String) m[1], null, share, (Boolean) m[2], null, null, null,
                        true, List.of()));
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

    /** The ids of the sheet's people with a hold whose dates include the day (the sheet's today), of the holds made while
     *  the sheet's today was on or before then. */
    private Set<Long> held(String sheetId, LocalDate on, java.sql.Date then) {
        java.sql.Date today = java.sql.Date.valueOf(on);
        return Set.copyOf(jdbc.queryForList("SELECT DISTINCT person_id FROM person_hold WHERE sheet_id = ?"
                + " AND from_date <= ? AND (to_date IS NULL OR to_date >= ?) AND made_on <= ?", Long.class, sheetId, today,
                today, then));
    }

    /** Whether each of the sheet's people with a retirement date given while the sheet's today was on or before then had
     *  one (the last given by then; an unretirement gives none). */
    private Map<Long, Boolean> retired(String sheetId, java.sql.Date then) {
        Map<Long, Boolean> retired = new HashMap<>();
        jdbc.query("SELECT person_id, from_date FROM person_retire_log WHERE sheet_id = ? AND made_on <= ? ORDER BY id",
                rs -> {
                    retired.put(rs.getLong("person_id"), rs.getDate("from_date") != null);
                }, sheetId, then);
        return retired;
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
            jdbc.update("INSERT INTO pool (sheet_id, name, made_on) VALUES (?, ?, ?)", sheetId, name,
                    sqlDate(sheets.today(sheetId)));
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
        jdbc.update("INSERT INTO pool_member (pool_id, member_pool_id, share, from_date, to_date, made_on)"
                + " VALUES (?, ?, ?, ?, ?, ?)", poolId, memberPoolId, share, sqlDate(from), sqlDate(to), madeOn(poolId));
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
            jdbc.update("INSERT INTO pool_member (pool_id, person_id, share, from_date, to_date, made_on)"
                    + " VALUES (?, ?, ?, ?, ?, ?)", poolId, personId, share, sqlDate(from), sqlDate(to), madeOn(poolId));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** The sheet's today of the pool's sheet. */
    private java.sql.Date madeOn(long poolId) {
        return sqlDate(sheets.today(jdbc.queryForObject("SELECT sheet_id FROM pool WHERE id = ?", String.class, poolId)));
    }

    private static java.sql.Date sqlDate(LocalDate date) {
        return date == null ? null : java.sql.Date.valueOf(date);
    }
}
