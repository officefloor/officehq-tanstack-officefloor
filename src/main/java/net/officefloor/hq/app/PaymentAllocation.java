package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * One line of a split payment: how much of the lump sum is applied to a single invoice
 * ({@code invoiceId}, {@code amount}). Part of the {@link NewClientPayment} request body — a client
 * pays one total and it is divided across several of their open invoices, one allocation per
 * invoice. Bound from JSON by Jackson (plain getters/setters).
 */
public class PaymentAllocation {

    private Long invoiceId;

    private BigDecimal amount;

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
