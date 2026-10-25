package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimFigures;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.grade.AddedGradeRepository;
import net.officefloor.hq.app.grade.RetiredGrades;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGrade;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonGrades;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** What a candidate rate would do: of the OPEN and PENDING claims whose person had the grade on the claim's date,
 *  not bound by a statement, how many would show a different due were the candidate the grade's rate on every date,
 *  and the sum of the differences (new due minus current due; n/a counts as 0.00). Nothing is saved. */
public class RespondWithRatePreview {
    public void respond(@Val Sheet sheet, @Val RatePreviewInput input, RateRepository rates, PersonRepository persons,
            ClaimRepository claims, PersonGradeRepository grades, AddedGradeRepository added,
            ObjectResponse<RatePreviewResponse> response) throws InvalidRateException {
        if (!RetiredGrades.isGrade(sheet.getId(), input.grade(), added)) {
            throw new InvalidRateException("The grade must be one of the sheet's grades.");
        }
        List<Rate> current = rates.findInChain(sheet.getId());
        List<Rate> candidate = new ArrayList<>(current.stream().filter(r -> !r.getGrade().equals(input.grade())).toList());
        candidate.add(new Rate(sheet.getId(), input.grade(), input.pct(), null, null));
        Map<Long, Person> people = persons.findListed(sheet.getId()).stream()
                .collect(Collectors.toMap(Person::getId, p -> p));
        Map<Long, List<PersonGrade>> dated = grades.datedOnSheet(sheet.getId());
        int count = 0;
        BigDecimal diff = BigDecimal.ZERO.setScale(2);
        for (Claim claim : claims.findBySheetIdOrderByIdDesc(sheet.getId())) {
            Person person = people.get(claim.getPersonId());
            if (person == null || !claim.getStatus().isOpen() || claim.isBound()) {
                continue;
            }
            PersonGrades personGrades = new PersonGrades(person.getGrade(), dated.getOrDefault(person.getId(), List.of()));
            if (!input.grade().equals(personGrades.on(claim.getDate()))) {
                continue;
            }
            BigDecimal was = new ClaimFigures(current, personGrades, sheet).due(claim);
            BigDecimal would = new ClaimFigures(candidate, personGrades, sheet).due(claim);
            BigDecimal before = was == null ? BigDecimal.ZERO : was;
            BigDecimal after = would == null ? BigDecimal.ZERO : would;
            if ((was == null) != (would == null) || before.compareTo(after) != 0) {
                count++;
                diff = diff.add(after.subtract(before));
            }
        }
        response.send(new RatePreviewResponse(count, diff.setScale(2).toPlainString()));
    }
}
