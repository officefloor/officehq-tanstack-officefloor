package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A member that passed validation: a person of the sheet not yet in the pool (memberPool null), or another pool of
 *  the sheet (personId null) that may nest there, with a share in (0.00, 100.00], and the optional
 *  from- and to-dates of the membership (null: unbounded; the to-date never before the from-date). */
public record ValidPoolMember(String pool, Long personId, String memberPool, String memberName, BigDecimal share,
        LocalDate from, LocalDate to, String user) {
}
