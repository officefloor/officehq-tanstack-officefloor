package net.officefloor.hq.app.person;

import java.math.BigDecimal;

/** One of a person's patrons: their name and their share of 100.00 (two decimals). */
public record PatronShare(String name, BigDecimal share) {
}
