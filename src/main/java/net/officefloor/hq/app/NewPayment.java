package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/payments}: the fields the user supplies when recording a payment
 * against an invoice — the id of the invoice it was paid on, how much ({@code amount}) and on what
 * day ({@code date}, an ISO {@code YYYY-MM-DD} string). Bound from the JSON payload by the Spring MVC
 * {@code @RequestBody} argument resolver.
 */
public class NewPayment {

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
