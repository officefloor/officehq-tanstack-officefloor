package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * An amount of money in one currency. Clients are billed in different currencies and their money is
 * never added across currencies, so a total that spans clients is carried as one of these per
 * currency (e.g. the home dashboard's outstanding figure, kept separate per currency). The amount
 * keeps its {@link BigDecimal} scale so the client formats it to two places in that currency.
 */
public record CurrencyAmount(String currency, BigDecimal amount) {
}
