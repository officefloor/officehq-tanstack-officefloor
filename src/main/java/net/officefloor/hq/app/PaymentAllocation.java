package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * One slice of a split payment: how much of a lump sum a client paid is put towards a particular
 * invoice. A client can pay one amount that is spread across several of their open invoices, and
 * each slice names the invoice it lands on and the amount allocated to it. Carried as a list on
 * {@link ClientPaymentForm}; the amount is a {@link BigDecimal} because it is money (fixed scale,
 * no float drift).
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
