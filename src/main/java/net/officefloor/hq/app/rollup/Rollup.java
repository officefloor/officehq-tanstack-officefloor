package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimShare;
import net.officefloor.hq.app.claim.ClaimShares;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.stereotype.Service;

/** Works out a sheet's figures the way the screen shows them (claimed, due-total, overdue-total, and their totals). */
@Service
public class Rollup {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final People people;
    private final Claims claims;
    private final Rates rates;
    private final Sheets sheets;
    private final Settings settings;
    private final Periods periods;
    private final ClaimShares shares;
    private final PoolCuts poolCuts;

    public Rollup(People people, Claims claims, Rates rates, Sheets sheets, Settings settings, Periods periods,
            ClaimShares shares, PoolCuts poolCuts) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.settings = settings;
        this.periods = periods;
        this.shares = shares;
        this.poolCuts = poolCuts;
    }

    /** The sheet's alert threshold; null when alerts are off. */
    public BigDecimal alertThreshold(String sheetId) {
        return settings.alertThreshold(sheetId);
    }

    public Figures figures(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Claim> counting = claims.list(sheetId).stream()
                .filter(c -> c.status().counts()).toList();
        Map<Long, List<ClaimShare>> shared = shares.of(sheetId);
        List<Person> listed = people.list(sheetId);
        Map<Long, Person> byId = new LinkedHashMap<>();
        listed.forEach(p -> byId.put(p.id(), p));
        Map<Long, BigDecimal> cuts = poolCuts.totals(sheetId);
        Map<Long, Figures.PersonFigures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = ZERO;
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        for (Person person : listed) {
            BigDecimal claimed = ZERO;
            BigDecimal due = ZERO;
            BigDecimal overdue = ZERO;
            BigDecimal settled = ZERO;
            int overdueCount = 0;
            boolean anyFigure = false;
            for (Claim claim : counting) {
                List<ClaimShare> claimShares = shared.getOrDefault(claim.id(), List.of());
                boolean own = claim.personId() == person.id();
                ClaimShare holding = own ? null
                        : claimShares.stream().filter(sh -> sh.personId() == person.id()).findFirst().orElse(null);
                Person owner = byId.get(claim.personId());
                if ((!own && holding == null) || owner == null || !countsClaimed(claim, owner, today)) {
                    continue;
                }
                // A claim that corrects another is left out of claimed; its due still counts.
                if (own && claim.corrects() == null) {
                    claimed = claimed.add(claim.amount());
                }
                if (!counts(claim, owner, today)) {
                    continue;
                }
                BigDecimal figure = figure(claim, owner, today);
                if (own && settings.overdue(claim, today)) {
                    overdueCount++;
                }
                // A holder counts their part of the figure; the claim's own person the rest.
                BigDecimal part = own ? (figure == null ? null : remainder(claim, figure, claimShares, byId))
                        : holderPart(claim, holding, byId.get(holding.personId()), rates, settings);
                // The score's settled-total leaves out corrections.
                if (part != null && claim.status() == ClaimStatus.SETTLED && claim.corrects() == null) {
                    settled = settled.add(part);
                }
                if (part != null) {
                    due = due.add(part);
                    anyFigure = true;
                    if (own && settings.overdue(claim, today)) {
                        overdue = overdue.add(figure);
                    }
                }
            }
            // With no rate today and no figure, the due-total is n/a.
            boolean none = !anyFigure && rates.on(sheetId, person.gradeOn(today), today) == null;
            // Otherwise it is held to the sheet's cap less their cut total.
            due = settings.capped(sheetId, due, cuts.getOrDefault(person.id(), ZERO));
            claimedTotal = claimedTotal.add(claimed);
            if (!none) {
                dues.add(due);
            }
            overdues.add(overdue);
            figures.put(person.id(), new Figures.PersonFigures(person.name(), claimed.toPlainString(),
                    none ? "n/a" : due.toPlainString(), overdue.toPlainString(), score(settled, overdueCount)));
        }
        return new Figures(figures, claimedTotal.toPlainString(), trimmed(dues).toPlainString(),
                trimmed(overdues).toPlainString());
    }

    /** What the claim's own person counts of its figure: the figure less each holder's part (one with none counting 0.00). */
    private BigDecimal remainder(Claim claim, BigDecimal figure, List<ClaimShare> claimShares, Map<Long, Person> byId) {
        return claimShares.stream().map(sh -> holderPart(claim, sh, byId.get(sh.personId()), rates, settings))
                .filter(java.util.Objects::nonNull).reduce(figure, BigDecimal::subtract);
    }

    /**
     * The holder's part of the claim (null when there is none): of a settled claim, its frozen parts (its frozen due times
     * the share divided by 100); otherwise its amount times the share divided by 100, times the rate of the holder's own
     * grade in force on the claim's date divided by 100, rounded like the claim (null when that grade has no rate).
     */
    public static BigDecimal holderPart(Claim claim, ClaimShare share, Person holder, Rates rates, Settings settings) {
        if (claim.status() == ClaimStatus.SETTLED) {
            return claim.settledDue() == null ? null : share.part(claim.settledDue());
        }
        BigDecimal rate = holder == null ? null : rates.forClaim(claim, holder.gradeOn(claim.date()));
        return share.part(claim.amount(), rate, settings.rounding(claim));
    }

    /** A person's score, as the screen shows it: their settled-total (corrections left out) / 100, minus 5 per overdue claim of theirs that counts, rounded half to even. */
    private static String score(BigDecimal settled, int overdueCount) {
        return settled.movePointLeft(2).subtract(BigDecimal.valueOf(5L * overdueCount))
                .setScale(0, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }

    /** A sheet total of per-person figures: with three or more, the single highest and lowest are left out. */
    private static BigDecimal trimmed(List<BigDecimal> figures) {
        List<BigDecimal> sorted = figures.stream().sorted().toList();
        List<BigDecimal> kept = sorted.size() >= 3 ? sorted.subList(1, sorted.size() - 1) : sorted;
        return kept.stream().reduce(ZERO, BigDecimal::add);
    }

    /**
     * Each person's due-total for the dates {@code from} to {@code to} inclusive, by person id in sheet order: the
     * figures (as shown now) of their claims due by a date then that counted by the status their log records at the end of
     * {@code to} (a claim added after it counts not at all), those without one skipped.
     */
    public Map<Long, BigDecimal> personDueTotals(String sheetId, LocalDate from, LocalDate to) {
        LocalDate today = sheets.today(sheetId);
        Map<Long, ClaimStatus> ended = claims.statusesOn(sheetId, to);
        List<Claim> dated = claims.list(sheetId).stream()
                .filter(c -> !c.dueBy().isBefore(from) && !c.dueBy().isAfter(to)).toList();
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (Person person : people.list(sheetId)) {
            BigDecimal total = ZERO;
            for (Claim claim : dated) {
                Claim then = claim.withStatus(ended.getOrDefault(claim.id(), claim.status()), claim.number(),
                        claim.settledDue(), claim.openedBy());
                // A hold leaves out only a claim that is open now.
                boolean left = claim.status().isOpen() ? !counts(then, person, today) : then.deferredOn(today);
                if (claim.personId() != person.id() || !then.status().counts() || left) {
                    continue;
                }
                BigDecimal figure = figure(claim, person, today);
                if (figure != null) {
                    total = total.add(figure);
                }
            }
            totals.put(person.id(), total);
        }
        return totals;
    }

    /**
     * Each person's carried figure for the period, by person id in sheet order: the figures (as shown now) of their claims
     * belonging (by due-by) to one of {@code periods} ending before it starts that were still OPEN or PENDING by the
     * status their log records at the end of its last day (a hold leaving out only a claim that is open now), those without
     * one skipped; and their opening balance for the period added.
     */
    public Map<Long, BigDecimal> personCarried(String sheetId, Period period, List<Period> periods) {
        LocalDate today = sheets.today(sheetId);
        Map<Long, ClaimStatus> ended = claims.statusesOn(sheetId, period.to());
        Map<Long, BigDecimal> openings = this.periods.openings(sheetId).getOrDefault(period.name(), Map.of());
        List<Claim> earlier = claims.list(sheetId).stream()
                .filter(c -> periods.stream().anyMatch(p -> p.contains(c.dueBy()) && p.to().isBefore(period.from()))).toList();
        Map<Long, BigDecimal> carried = new LinkedHashMap<>();
        for (Person person : people.list(sheetId)) {
            BigDecimal total = ZERO;
            for (Claim claim : earlier) {
                Claim then = claim.withStatus(ended.getOrDefault(claim.id(), claim.status()), claim.number(),
                        claim.settledDue(), claim.openedBy());
                boolean left = claim.status().isOpen() ? !counts(then, person, today) : then.deferredOn(today);
                if (claim.personId() != person.id() || !then.status().isOpen() || left) {
                    continue;
                }
                BigDecimal figure = figure(claim, person, today);
                if (figure != null) {
                    total = total.add(figure);
                }
            }
            carried.put(person.id(), total.add(openings.getOrDefault(person.id(), ZERO)));
        }
        return carried;
    }

    /**
     * Each person's open figure at the end of {@code end}, by person id in sheet order: the figures (as shown now) of
     * their claims that count belonging (by due-by) to one of {@code periods} that were OPEN or PENDING by the status
     * their log records at the end of {@code end} (a hold leaving out only a claim that is open now), those without one
     * skipped.
     */
    public Map<Long, BigDecimal> personOpenAt(String sheetId, List<Period> periods, LocalDate end) {
        LocalDate today = sheets.today(sheetId);
        Map<Long, ClaimStatus> ended = claims.statusesOn(sheetId, end);
        List<Claim> within = claims.list(sheetId).stream()
                .filter(c -> periods.stream().anyMatch(p -> p.contains(c.dueBy()))).toList();
        Map<Long, BigDecimal> open = new LinkedHashMap<>();
        for (Person person : people.list(sheetId)) {
            BigDecimal total = ZERO;
            for (Claim claim : within) {
                Claim then = claim.withStatus(ended.getOrDefault(claim.id(), claim.status()), claim.number(),
                        claim.settledDue(), claim.openedBy());
                boolean left = claim.status().isOpen() ? !counts(then, person, today) : then.deferredOn(today);
                if (claim.personId() != person.id() || !then.status().isOpen() || left) {
                    continue;
                }
                BigDecimal figure = figure(claim, person, today);
                if (figure != null) {
                    total = total.add(figure);
                }
            }
            open.put(person.id(), total);
        }
        return open;
    }

    /** The claim's figure as the screen shows it on the sheet's today (null: n/a), whatever its status. */
    public BigDecimal figure(Claim claim) {
        Person person = people.find(claim.sheetId(), claim.personId());
        return person == null ? claim.settledDue() : figure(claim, person, sheets.today(claim.sheetId()));
    }

    /**
     * Whether the (OPEN, PENDING or SETTLED) claim counts: a person's open claims count in no live total once
     * {@code today} reaches their retirement date, unless a statement bound them; nor does an OPEN claim deferred on
     * {@code today}. A hold (the claim's or its person's) does not leave a claim out: it only takes the person's cut
     * out of the pools.
     */
    public static boolean counts(Claim claim, Person person, LocalDate today) {
        if (claim.deferredOn(today)) {
            return false;
        }
        boolean bound = claim.stated();
        return !(claim.status().isOpen() && !bound && person.retiredOn(today));
    }

    /**
     * Whether the (OPEN, PENDING or SETTLED) claim counts toward claimed: as {@link #counts}, except that a hold (the
     * claim's or its person's) does not leave it out.
     */
    public static boolean countsClaimed(Claim claim, Person person, LocalDate today) {
        if (claim.deferredOn(today)) {
            return false;
        }
        return !(claim.status().isOpen() && !claim.stated() && person.retiredOn(today));
    }

    /**
     * The claim's figure (null when it has none): a settled claim's frozen due, otherwise the due its person's
     * last statement bound it to as shown then (late charge and floor included), otherwise its amount times its person's
     * grade's rate on its date, rounded by the policy date, raised to the floor, with its late charge on {@code today} (worked out on the floored figure) added on top.
     * A PENDING claim is bound like any other claim that counts.
     */
    private BigDecimal figure(Claim claim, Person person, LocalDate today) {
        if (claim.settledDue() != null) {
            return claim.settledDue();
        }
        if (claim.stated()) {
            return claim.statedDue();
        }
        return settings.withLate(claim, live(claim, person), today);
    }

    /** The claim's live rate figure before any late charge (not floored). */
    private BigDecimal live(Claim claim, Person person) {
        BigDecimal rate = rates.forClaim(claim, person.gradeOn(claim.date()));
        return settings.live(claim, rate);
    }
}
