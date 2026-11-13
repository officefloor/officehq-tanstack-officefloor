package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/**
 * Works out a sheet's figures as the screen shows them: a person's claimed is their counting claims' amounts
 * added up; their due-total those claims' figures added up, n/a when their grade has no rate on the sheet's
 * today and no counting claim keeps a figure. The sheet's totals add these up, skipping n/a.
 */
@Service
public class RollupService {

    private static final String NA = "n/a";

    private final PersonRepository people;
    private final ClaimRepository claims;
    private final RateRepository rates;
    private final SheetRepository sheets;

    public RollupService(PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets) {
        this.people = people;
        this.claims = claims;
        this.rates = rates;
        this.sheets = sheets;
    }

    /** The sheet's figures as the store holds them now. */
    public Rollup compute(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        List<Rate> sheetRates = rates.findAll(sheetId);
        List<Claim> sheetClaims = claims.findAll(sheetId);
        Map<Long, Rollup.Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal dueTotal = BigDecimal.ZERO.setScale(2);
        for (Person person : people.findAll(sheetId)) {
            BigDecimal claimed = BigDecimal.ZERO.setScale(2);
            BigDecimal due = BigDecimal.ZERO.setScale(2);
            boolean kept = false;
            for (Claim claim : sheetClaims) {
                if (claim.personId() != person.id() || !claim.counts()) {
                    continue;
                }
                claimed = claimed.add(claim.amount());
                kept |= claim.due() != null || (claim.stated() && claim.statedDue() != null);
                BigDecimal figure = claim.figure(Rate.inForce(sheetRates, person.grade(), claim.date()));
                if (figure != null) {
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
