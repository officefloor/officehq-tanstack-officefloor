package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A validated member to add: the pool's name, the member — a person's id, or else (personId null) the name of another
 * pool — their share (two decimals, 0.00 < share <= 100.00) and the membership's from-date and to-date (each null when
 * not given; the to-date never before the from-date).
 */
public record PoolMemberInput(String pool, Long personId, String memberPool, BigDecimal share, LocalDate from,
        LocalDate to) {
}
