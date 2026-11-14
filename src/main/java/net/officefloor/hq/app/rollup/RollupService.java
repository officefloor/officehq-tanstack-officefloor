package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimShare;
import net.officefloor.hq.app.claim.ClaimShareRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.pool.Pools;
import net.officefloor.hq.app.setting.Cap;
import net.officefloor.hq.app.setting.DatedSettings;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/**
 * Works out a sheet's figures as the screen shows them: a person's claimed is their counting claims' amounts
 * added up (a held claim's too, in every figure: a hold only takes away a pool cut; none once their retirement date is reached), leaving out corrections; their due-total those claims' dues (live figures rounded by its policy date, plus
 * any late charge, at the late percent in force on each 30-day boundary, on the overdue ones, then raised to the floor in force on each claim's date) added up, at most the cap in force on the sheet's today less their cut total (their pool cuts added up; not below 0.00); n/a when their grade on the sheet's today has no
 * rate there and no counting claim has a figure (kept, or at the rate for the grade in force on its date). Their overdue-total
 * is the dues of their overdue counting claims added up (skipping n/a, not capped). The sheet's totals add these up, skipping n/a;
 * its due-total and overdue-total leave out the single highest and lowest person's figure when three or more have one.
 * A shared claim's due is parted: each holder's due-total counts the due times their share / 100 (half to even, while
 * the claim counts for its own person), and its own person's the due less those parts; its overdue figure is not parted.
 */
@Service
public class RollupService {

    private static final String NA = "n/a";

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final RateRepository rates;
    private final SheetRepository sheets;
    private final SettingRepository settings;
    private final ClaimShareRepository shares;
    private final PoolCuts poolCuts;

    public RollupService(PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings, ClaimShareRepository shares, PoolCuts poolCuts) {
        this.poolCuts = poolCuts;
        this.shares = shares;
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
        this.settings = settings;
    }

    /** The sheet's figures as the store holds them now. */
    public Rollup compute(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Rate> sheetRates = rates.findAll(sheetId);
        DatedSettings dated = settings.dated(sheetId);
        BigDecimal cap = dated.capOn(today);
        Pools pools = cap == null ? null : poolCuts.live(sheetId, today);
        List<Claim> sheetClaims = claims.findAll(sheetId);
        Rounding rounding = settings.rounding(sheetId, today, sheetClaims);
        Map<Long, Rollup.Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        List<BigDecimal> dues = new ArrayList<>();
        List<BigDecimal> overdues = new ArrayList<>();
        List<Person> sheetPeople = people.findAll(sheetId);
        Map<Long, List<ClaimShare>> shared = shares.findAll(sheetId);
        for (Person person : sheetPeople) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            BigDecimal overdue = BigDecimal.ZERO.setScale(2);
            BigDecimal settled = BigDecimal.ZERO.setScale(2);
            int overdueCount = 0;
            boolean kept = false;
            for (Claim claim : sheetClaims) {
                if (claim.personId() != person.id()) {
                    continue;
                }
                if (!claim.correction() && person.countsClaimed(claim, today)) {
                    claimed = claimed.add(claim.amount());
                }
                if (!person.counts(claim, today)) {
                    continue;
                }
                kept |= claim.keepsFigure();
                BigDecimal figure = claim.due(Rate.inForce(sheetRates, person.gradeOn(claim.date()), claim),
                        dated.floorOn(claim.date()), dated.lateCharge(today, person), rounding);
                boolean late = claim.overdue(today, dated.graceFor(person, claim));
                if (late) {
                    overdueCount++;
                }
                if (figure != null) {
                    kept = true;
                    due = due.add(ClaimShare.ownPart(figure, shared.getOrDefault(claim.id(), List.of())));
                    if (late) {
                        overdue = overdue.add(figure);
                    }
                    if (claim.status() == ClaimStatus.SETTLED && !claim.correction()) {
                        settled = settled.add(figure);
                    }
                }
            }
            for (Claim claim : sheetClaims) {
                for (ClaimShare share : shared.getOrDefault(claim.id(), List.of())) {
                    Person owner = share.personId() == person.id() ? find(sheetPeople, claim.personId()) : null;
                    if (owner == null || !owner.counts(claim, today)) {
                        continue;
                    }
                    BigDecimal figure = claim.due(Rate.inForce(sheetRates, owner.gradeOn(claim.date()), claim),
                            dated.floorOn(claim.date()), dated.lateCharge(today, owner), rounding);
                    if (figure != null) {
                        kept = true;
                        due = due.add(share.part(figure));
                    }
                }
            }
            boolean na = Rate.inForce(sheetRates, person.grade(), today) == null && !kept;
            due = Cap.apply(cap, pools == null ? BigDecimal.ZERO : pools.cutTotalOf(person.id()), due);
            claimedTotal = claimedTotal.add(claimed);
            if (!na) {
                dues.add(due);
            }
            overdues.add(overdue);
            figures.put(person.id(), new Rollup.Figures(person.name(), show(claimed), na ? NA : show(due), show(overdue),
                    score(settled, overdueCount)));
        }
        return new Rollup(figures, show(claimedTotal), show(trimmedTotal(dues)), show(trimmedTotal(overdues)),
                settings.alertThreshold(sheetId));
    }

    private static Person find(List<Person> people, long id) {
        return people.stream().filter((p) -> p.id() == id).findFirst().orElse(null);
    }

    /**
     * A person's score: their settled counting claims' dues (leaving out corrections) / 100, minus 5 per overdue counting claim, rounded half to
     * even to a whole number.
     */
    private static String score(BigDecimal settled, int overdueCount) {
        return settled.subtract(BigDecimal.valueOf(500L * overdueCount)).movePointLeft(2)
                .setScale(0, RoundingMode.HALF_EVEN).toPlainString();
    }

    /** The figures added up, leaving out the single highest and single lowest when there are three or more. */
    private static BigDecimal trimmedTotal(List<BigDecimal> values) {
        List<BigDecimal> sorted = new ArrayList<>(values);
        sorted.sort(null);
        if (sorted.size() >= 3) {
            sorted = sorted.subList(1, sorted.size() - 1);
        }
        return sorted.stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    private static String show(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
