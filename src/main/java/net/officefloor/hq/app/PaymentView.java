package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * What the API exposes for one payment against an invoice: the shape the front-end renders into the
 * invoice's payments table. The amount keeps its scale (a {@link BigDecimal}) so the client formats
 * money to two places; the date is the ISO day it was paid (yyyy-MM-dd), shown as-is.
 */
public record PaymentView(Long id, Long invoiceId, BigDecimal amount, String date) {

    public static PaymentView of(InvoicePayment payment) {
        return new PaymentView(payment.getId(), payment.getInvoiceId(), payment.getAmount(),
                payment.getPaidDate().toString());
    }
}
