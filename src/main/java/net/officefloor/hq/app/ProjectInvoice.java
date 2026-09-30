package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * An invoice as a project's list shows it: every stored {@link Invoice} field plus the money already
 * settled against it ({@code paid}, the sum of its {@link Payment}s) and how much is still owed
 * ({@code due} = amount - paid). The paid/due split is derived per request in {@link InvoicesGet}
 * rather than stored, so it always reflects the current payments. Additive over the raw invoice
 * shape — existing readers keep using {@code amount}; the new ones read {@code due}.
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

    public ProjectInvoice(Invoice invoice, BigDecimal paid) {
        this.id = invoice.getId();
        this.projectId = invoice.getProjectId();
        this.amount = invoice.getAmount();
        this.status = deriveStatus(invoice.getStatus(), invoice.getAmount(), paid);
        this.issuedDate = invoice.getIssuedDate();
        this.dueDate = invoice.getDueDate();
        this.paid = paid;
        this.due = invoice.getAmount().subtract(paid);
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
}
