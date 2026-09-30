package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * The request body for recording a payment against an invoice: how much was paid (amount) and when
 * (date, an ISO date string). The invoice id comes from the path, and the payment id is generated.
 * Bound from the POST JSON body via {@code @RequestBody}.
 */
public class NewPayment {

    private BigDecimal amount;

    private String date;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
