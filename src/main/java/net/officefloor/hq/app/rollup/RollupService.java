package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.claim.Rounding;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/**
 * Works out a sheet's figures as the screen shows them: a person's claimed is their counting claims' amounts
 * added up; their due-total those claims' dues (live figures raised to the sheet's floor and rounded by its policy date, plus
 * any late charge on the overdue ones) added up, n/a when their grade on the sheet's today has no
 * rate there and no counting claim has a figure (kept, or at the rate for the grade in force on its date). The sheet's totals add these up, skipping n/a.
 */
@Service
public class RollupService {

    private static final String NA = "n/a";

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final RateRepository rates;
    private final SheetRepository sheets;
    private final SettingRepository settings;

    public RollupService(PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings) {
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
        BigDecimal floor = settings.floor(sheetId);
        LateCharge late = settings.lateCharge(sheetId, today);
        List<Claim> sheetClaims = claims.findAll(sheetId);
        Rounding rounding = settings.rounding(sheetId, today, sheetClaims);
        Map<Long, Rollup.Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal dueTotal = BigDecimal.ZERO.setScale(2);
        for (Person person : people.findAll(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            boolean kept = false;
            for (Claim claim : sheetClaims) {
                if (claim.personId() != person.id() || !person.counts(claim)) {
                    continue;
                }
                claimed = claimed.add(claim.amount());
                kept |= claim.keepsFigure();
                BigDecimal figure = claim.due(Rate.inForce(sheetRates, person.gradeOn(claim.date()), claim.date()), floor, late, rounding);
                if (figure != null) {
                    kept = true;
                    due = due.add(figure);
                }
            }
            boolean na = Rate.inForce(sheetRates, person.grade(), today) == null && !kept;
            claimedTotal = claimedTotal.add(claimed);
            if (!na) {
                dueTotal = dueTotal.add(due);
            }
            figures.put(person.id(), new Rollup.Figures(person.name(), show(claimed), na ? NA : show(due)));
        }
        return new Rollup(figures, show(claimedTotal), show(dueTotal));
    }

    private static String show(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
