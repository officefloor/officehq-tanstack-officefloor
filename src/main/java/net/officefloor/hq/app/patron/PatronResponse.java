package net.officefloor.hq.app.patron;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A person and one of their patrons, with the patron's share, as the screen reads them. A person's 100.00 is split
 * equally among their patrons in the order they were named, each share rounded half to even to two decimals; the
 * remainder (100.00 less the shares added up) goes to the first named.
 */
public record PatronResponse(long personId, long patronId, String share) {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    /** The patrons (by person, then in the order named) with their shares, in the same order. */
    public static List<PatronResponse> from(List<Patron> patrons) {
        Map<Long, List<Patron>> byPerson = new LinkedHashMap<>();
        for (Patron patron : patrons) {
            byPerson.computeIfAbsent(patron.personId(), k -> new ArrayList<>()).add(patron);
        }
        List<PatronResponse> responses = new ArrayList<>();
        for (List<Patron> named : byPerson.values()) {
            BigDecimal share = HUNDRED.divide(BigDecimal.valueOf(named.size()), 2, RoundingMode.HALF_EVEN);
            BigDecimal first = HUNDRED.subtract(share.multiply(BigDecimal.valueOf(named.size()))).add(share);
            for (int i = 0; i < named.size(); i++) {
                Patron patron = named.get(i);
                responses.add(new PatronResponse(patron.personId(), patron.patronId(),
                        (i == 0 ? first : share).toPlainString()));
            }
        }
        return responses;
    }
}
