package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * An invoice as a project's list shows it: every stored {@link Invoice} field plus the money already
 * settled against it ({@code paid}, the sum of its {@link Payment}s) and how much is still owed
 * ({@code due} = discounted amount - paid). The paid/due split is derived per request in
 * {@link InvoicesGet} rather than stored, so it always reflects the current payments. Additive over
 * the raw invoice shape — existing readers keep using {@code amount}; the new ones read {@code due}.
 *
 * What is OWED is the invoice's percentage discount applied on top of the stored {@code amount} (the
 * subtotal), the same figure the invoice's discount breakdown shows — so the discount flows through
 * everywhere money owed is surfaced (the invoice row's due, the client statement, the dashboard
 * outstanding total). The stored {@code amount} itself is left as the face (pre-discount) figure.
 *
 * The {@code status} is likewise DERIVED from the payments rather than flipped by hand: once some
 * (but not all) of the amount is paid it reads {@code PARTIAL}, once the amount is covered it reads
 * {@code PAID}; with nothing paid it stays at the invoice's stored lifecycle stage (DRAFT/SENT).
 * See {@link #deriveStatus}.
 */
public class ProjectInvoice {

    private final Long id;
    private final Long projectId;
    private final BigDecimal amount;
    private final String status;
    private final String issuedDate;
    private final String dueDate;
    private final BigDecimal paid;
    private final BigDecimal due;
    private final String currency;

    public ProjectInvoice(Invoice invoice, BigDecimal paid) {
        this(invoice, paid, "USD");
    }

    /**
     * As {@link #ProjectInvoice(Invoice, BigDecimal)} but carrying the currency the invoice's client
     * pays in, so the front-end renders the amount and due in that currency (e.g. USD or EUR).
     */
    public ProjectInvoice(Invoice invoice, BigDecimal paid, String currency) {
        BigDecimal owed = owedAmount(invoice.getAmount(), invoice.getDiscountPct());
        this.id = invoice.getId();
        this.projectId = invoice.getProjectId();
        this.amount = invoice.getAmount();
        this.status = deriveStatus(invoice.getStatus(), owed, paid);
        this.issuedDate = invoice.getIssuedDate();
        this.dueDate = invoice.getDueDate();
        this.paid = paid;
        this.due = owed.subtract(paid);
        this.currency = currency;
    }

    /**
     * What a client actually owes for an invoice: the stored {@code amount} (the subtotal, before any
     * discount) less the percentage discount taken off it. The discount is {@code amount * pct / 100}
     * rounded to the cent, matching {@link InvoiceDiscount}, so the invoice's due, the statement and
     * the dashboard all agree with the invoice's discount breakdown. A null amount or percentage is
     * treated as zero.
     */
    static BigDecimal owedAmount(BigDecimal amount, BigDecimal discountPct) {
        BigDecimal amt = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal pct = discountPct == null ? BigDecimal.ZERO : discountPct;
        BigDecimal discount = amt.multiply(pct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return amt.subtract(discount);
    }

    /**
     * Work out an invoice's status from what has been paid against it, so it is never flipped by
     * hand: {@code PAID} once the payments cover the amount, {@code PARTIAL} once some (but not all)
     * is paid, otherwise the invoice's stored lifecycle stage ({@code DRAFT}/{@code SENT} — a sent,
     * unpaid invoice reads SENT).
     */
    static String deriveStatus(String stored, BigDecimal amount, BigDecimal paid) {
        BigDecimal amt = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal pd = paid == null ? BigDecimal.ZERO : paid;
        if (amt.signum() > 0 && pd.compareTo(amt) >= 0) {
            return "PAID";
        }
        if (pd.signum() > 0) {
            return "PARTIAL";
        }
        return stored;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getIssuedDate() {
        return issuedDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public BigDecimal getPaid() {
        return paid;
    }

    public BigDecimal getDue() {
        return due;
    }

    /** The currency the invoice's client pays in (an ISO code, e.g. USD or EUR). */
    public String getCurrency() {
        return currency;
    }
}
