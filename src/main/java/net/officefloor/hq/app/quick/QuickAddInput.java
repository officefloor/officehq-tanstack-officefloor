package net.officefloor.hq.app.quick;

import java.math.BigDecimal;

/** A validated quick-add: the name (trimmed) and the amount. */
public record QuickAddInput(String name, BigDecimal amount) {
}
