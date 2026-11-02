package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.grade.Grades;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Dated;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Responds with a preview of a candidate rate applied to a grade on every date: the OPEN and PENDING claims of
 * people on that grade (the one in force on the claim's date) whose due would differ, and the sum of the
 * differences. A claim bound by a statement keeps its figure; a settled one is not counted. Nothing is written.
 */
public class RespondWithRatePreview {
    private static final Pattern PCT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");

    public void respond(@PathVariable("sheet") String sheet,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "pct", required = false) String pct, Rates rates, People people, Claims claims,
            Settings settings, SheetClock clock, Grades grades, ObjectResponse<RatePreview> response) throws InvalidRateException {
        String g = grade == null ? "" : grade.trim();
        String p = pct == null ? "" : pct.trim();
        if (!grades.isGrade(sheet, g)) {
            throw new InvalidRateException("Grade must be one of the sheet's grades");
        }
        if (!PCT.matcher(p).matches()) {
            throw new InvalidRateException("The rate must be a percent of 0.00 or more with up to two decimals");
        }
        List<Rate> current = rates.list(sheet);
        List<Rate> candidate = new ArrayList<>(current.stream().filter((r) -> !r.grade().equals(g)).toList());
        candidate.add(new Rate(g, new BigDecimal(p).setScale(2).toPlainString(), null, null));
        LocalDate today = clock.today(sheet);
        Dated floor = settings.floor(sheet);
        Late late = settings.late(sheet, today);
        List<Claim> all = claims.list(sheet);
        int count = 0;
        BigDecimal diff = BigDecimal.ZERO.setScale(2);
        for (Person person : people.list(sheet)) {
            for (Claim claim : all) {
                if (claim.person() != person.id()
                        || (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING)
                        || claim.bound() || !g.equals(person.gradeOn(claim.date()))) {
                    continue;
                }
                BigDecimal was = claim.due(current, g, floor, late);
                BigDecimal now = claim.due(candidate, g, floor, late);
                if (was == null ? now != null : now == null || was.compareTo(now) != 0) {
                    count++;
                    diff = diff.add(now == null ? BigDecimal.ZERO : now)
                            .subtract(was == null ? BigDecimal.ZERO : was);
                }
            }
        }
        response.send(new RatePreview(count, diff.setScale(2).toPlainString()));
    }
}
