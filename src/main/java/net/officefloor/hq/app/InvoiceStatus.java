package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * Works out an invoice's status from the payments recorded against it, instead of a hand-set flag: a
 * DRAFT (not yet sent) keeps its stored lifecycle status, but once an invoice is SENT its status is
 * derived — SENT while nothing is paid, PARTIAL once some of the amount is covered, and PAID once the
 * payments cover it in full. Kept in one place so the per-project list, the invoice detail page and
 * the all-invoices view all agree on the same rule.
 */
public final class InvoiceStatus {

    private InvoiceStatus() {
    }

    /**
     * @param baseStatus the invoice's stored lifecycle status (e.g. DRAFT, SENT).
     * @param amount     what the invoice totals (its line items), null-safe.
     * @param paid       the sum of its payments, null-safe.
     * @return the status to show: the stored status for anything not yet SENT; otherwise SENT /
     *         PARTIAL / PAID worked out from the payments.
     */
    public static String derive(String baseStatus, BigDecimal amount, BigDecimal paid) {
        if (!"SENT".equals(baseStatus)) {
            // Not yet sent — payments do not apply; keep the lifecycle status as stored.
            return baseStatus;
        }
        BigDecimal paidAmount = paid != null ? paid : BigDecimal.ZERO;
        if (paidAmount.signum() <= 0) {
            return "SENT";
        }
        if (amount != null && paidAmount.compareTo(amount) >= 0) {
            return "PAID";
        }
        return "PARTIAL";
    }
}
