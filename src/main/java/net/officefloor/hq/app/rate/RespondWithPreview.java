package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigure;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * What a candidate rate would do: for the OPEN and PENDING claims of people on the grade (the one
 * in force on the claim's date), how many would show a different due were the grade's rate the
 * candidate on every date, and the differences (new due minus current due) added up. A claim bound
 * by a statement keeps its figure; n/a counts as nothing. Nothing is saved.
 */
public class RespondWithPreview {
    public void respond(@Val Sheet sheet, @Val PreviewInput preview, PersonRepository people,
            ClaimRepository claims, RateRepository rates, SettingsRepository settingsRepository,
            ObjectResponse<PreviewResponse> response) {
        LocalDate today = sheet.today();
        Settings settings = settingsRepository.find(sheet.id());
        List<Rate> current = rates.findAll(sheet.id());
        List<Rate> candidate = new ArrayList<>(current.stream()
                .filter((r) -> !r.grade().equals(preview.grade())).toList());
        candidate.add(new Rate(sheet.id(), preview.grade(), preview.pct(), null, null));
        Map<Long, Person> byId = people.findAll(sheet.id()).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        int count = 0;
        BigDecimal diff = BigDecimal.ZERO.setScale(2);
        for (Claim claim : claims.findAll(sheet.id())) {
            if (!claim.isOpen() || claim.isBound()) continue;
            Person person = byId.get(claim.getPersonId());
            if (person == null) continue;
            String grade = person.gradeOn(claim.getDate());
            if (!preview.grade().equals(grade)) continue;
            BigDecimal was = ClaimFigure.withLate(claim, current, grade, settings, today);
            BigDecimal now = ClaimFigure.withLate(claim, candidate, grade, settings, today);
            if (Objects.equals(was, now)) continue;
            count++;
            diff = diff.add(orZero(now)).subtract(orZero(was));
        }
        response.send(new PreviewResponse(count, diff.setScale(2).toPlainString()));
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
