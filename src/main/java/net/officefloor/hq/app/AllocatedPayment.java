package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for {@code POST /api/payments/allocate}: one lump payment a client has made, split
 * across several of their invoices. The {@code amount} is the whole sum handed over and the
 * {@code date} is when; {@code allocations} says how that sum is spread — one entry per invoice with
 * the share applied to it. The shares must add up to {@code amount} (see {@link PaymentsAllocatePost}).
 * Bound from the JSON payload by the Spring MVC {@code @RequestBody} argument resolver.
 */
public class AllocatedPayment {

    private BigDecimal amount;

    private String date;

    private List<Allocation> allocations;

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

    public List<Allocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<Allocation> allocations) {
        this.allocations = allocations;
    }

    /** One share of the lump payment: which invoice it goes against and how much of the sum it takes. */
    public static class Allocation {

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
}
