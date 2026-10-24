package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigures;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * The sheet's figures at one moment, as the screen shows them: each person's claimed and due-total, in the
 * order they were added, and the sheet's claimed-total and due-total (everyone's added up, skipping n/a).
 */
public class Rollup {

    /** One person's figures. */
    public record Figures(String name, String claimed, String dueTotal) {
    }

    private final Map<Long, Figures> people;
    private final String claimedTotal;
    private final String dueTotal;

    private Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal) {
        this.people = people;
        this.claimedTotal = claimedTotal;
        this.dueTotal = dueTotal;
    }

    /** Works out the sheet's figures now; a sheet not yet stored has none. */
    public static Rollup of(String sheetId, SheetRepository sheets, PersonRepository persons,
            ClaimRepository claims, RateRepository rates) {
        String id = sheetId.trim();
        Optional<Sheet> sheet = sheets.findById(id);
        Map<Long, Figures> people = new LinkedHashMap<>();
        BigDecimal claimedTotal = BigDecimal.ZERO;
        BigDecimal dueTotal = BigDecimal.ZERO;
        if (sheet.isPresent()) {
            List<Claim> all = claims.findBySheetIdOrderByIdDesc(id);
            for (Person person : persons.findBySheetIdOrderByIdAsc(id)) {
                List<Claim> mine = all.stream().filter(c -> person.getId().equals(c.getPersonId())).toList();
                BigDecimal claimed = mine.stream().filter(ClaimFigures::counts).map(Claim::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
                String due = new ClaimFigures(rates.findBySheetIdAndGrade(id, person.getGrade()))
                        .dueTotal(mine, sheet.get().getToday());
                people.put(person.getId(), new Figures(person.getName(), claimed.toPlainString(), due));
                claimedTotal = claimedTotal.add(claimed);
                if (!"n/a".equals(due)) {
                    dueTotal = dueTotal.add(new BigDecimal(due));
                }
            }
        }
        return new Rollup(people, claimedTotal.setScale(2).toPlainString(), dueTotal.setScale(2).toPlainString());
    }

    /** The audit lines telling how the figures moved from before to now: one per person whose figures changed
     *  (or who is new), then the totals when they changed; none when nothing moved. */
    public List<String> changesSince(Rollup before, String sheetId, String user) {
        String head = " sheet=" + sheetId.trim() + " by=" + user;
        List<String> lines = new ArrayList<>();
        people.forEach((id, now) -> {
            Figures was = before.people.get(id);
            if (was == null || !was.claimed().equals(now.claimed()) || !was.dueTotal().equals(now.dueTotal())) {
                lines.add("ROLLUP" + head + " person=" + now.name() + " claimed=" + now.claimed()
                        + " due-total=" + now.dueTotal());
            }
        });
        if (!before.claimedTotal.equals(claimedTotal) || !before.dueTotal.equals(dueTotal)) {
            lines.add("TOTALS" + head + " claimed-total=" + claimedTotal + " due-total=" + dueTotal);
        }
        return lines;
    }
}
