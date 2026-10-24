package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BindingLogRepository extends JpaRepository<BindingLog, Long> {

    /** The person's bindings, in the order made. */
    List<BindingLog> findByPersonIdOrderByIdAsc(Long personId);

    /** The person's claims bound at the end of a date, each to its figure (null: n/a), by claim id: those of the
     *  last binding made while the sheet's today was on or before it; none when there was none. */
    default Map<Long, BigDecimal> boundAsOf(Long personId, LocalDate date) {
        Map<Long, BigDecimal> bound = new HashMap<>();
        findByPersonIdOrderByIdAsc(personId).stream().filter(b -> !b.getMadeOn().isAfter(date))
                .reduce((a, b) -> b)
                .ifPresent(last -> last.getBound().forEach(c -> bound.put(c.getClaimId(), c.getStatedDue())));
        return bound;
    }
}
