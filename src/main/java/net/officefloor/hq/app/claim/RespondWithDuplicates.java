package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's duplicate claims: for each person, date and amount with two or more claims (VOID and
 *  DELETED left out), one line <name> <date> <amount> x<count>; people in list order, then by date,
 *  then by amount. Reads only; records nothing. */
public class RespondWithDuplicates {
    public void respond(@PathVariable("sheetId") String sheetId, People people, Claims claims,
            ObjectResponse<List<String>> response) {
        Map<Long, TreeMap<LocalDate, TreeMap<BigDecimal, Integer>>> counts = new HashMap<>();
        for (Claim claim : claims.onSheet(sheetId)) {
            if (claim.getStatus() == ClaimStatus.VOID || claim.getStatus() == ClaimStatus.DELETED) continue;
            counts.computeIfAbsent(claim.getPersonId(), k -> new TreeMap<>())
                    .computeIfAbsent(claim.getDate(), k -> new TreeMap<>())
                    .merge(claim.getAmount().setScale(2), 1, Integer::sum);
        }
        List<String> lines = new ArrayList<>();
        for (Person person : people.onSheet(sheetId)) {
            TreeMap<LocalDate, TreeMap<BigDecimal, Integer>> byDate = counts.get(person.getId());
            if (byDate == null) continue;
            byDate.forEach((date, byAmount) -> byAmount.forEach((amount, n) -> {
                if (n >= 2) lines.add(person.getName() + " " + date + " " + amount.toPlainString() + " x" + n);
            }));
        }
        response.send(lines);
    }
}
