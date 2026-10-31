package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * The sheet's duplicate claims: for each person, date and amount with two or more claims (VOID and DELETED left
 * out), one line "<name> <date> <amount> x<count>"; people in list order, then by date, then by amount.
 */
public class LoadDuplicateClaims {
    private record Key(LocalDate date, BigDecimal amount) {}

    private static final Comparator<Key> ORDER = Comparator.comparing(Key::date).thenComparing(Key::amount);

    public void service(@PathVariable("sheetId") String sheetId, People people, Claims claims,
            ObjectResponse<List<String>> response) {
        Map<Long, Map<Key, Integer>> byPerson = new java.util.HashMap<>();
        for (Claim claim : claims.list(sheetId)) {
            if (claim.getStatus() == ClaimStatus.VOID || claim.getStatus() == ClaimStatus.DELETED) continue;
            Key key = new Key(claim.getDate(), claim.getAmount().setScale(2, RoundingMode.HALF_UP));
            byPerson.computeIfAbsent(claim.getPersonId(), id -> new TreeMap<>(ORDER)).merge(key, 1, Integer::sum);
        }
        List<String> lines = new ArrayList<>();
        for (Person person : people.list(sheetId)) {
            Map<Key, Integer> counts = byPerson.get(person.getId());
            if (counts == null) continue;
            counts.forEach((key, count) -> {
                if (count >= 2) lines.add(person.getName() + " " + key.date() + " " + key.amount().toPlainString() + " x" + count);
            });
        }
        response.send(lines);
    }
}
