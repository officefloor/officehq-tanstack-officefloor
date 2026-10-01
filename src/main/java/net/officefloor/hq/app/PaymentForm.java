package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * The request body for recording a payment against an invoice: which invoice, how much was paid, and
 * the date it was paid (an ISO yyyy-MM-dd string).
 */
public class PaymentForm {

    private Long invoiceId;
    private BigDecimal amount;
    private String date;

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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
