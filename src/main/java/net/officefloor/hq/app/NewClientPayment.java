package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;

/**
 * The request body for recording ONE lump payment a client made and splitting it across several of
 * their open invoices: the total {@code amount}, the {@code date} it was paid (an ISO date string),
 * and the per-invoice {@code allocations} that say how much of the total is applied to each invoice.
 * The client id comes from the path. Bound from the POST JSON body via {@code @RequestBody}
 * (Jackson maps the nested allocation list).
 */
public class NewClientPayment {

    private BigDecimal amount;

    private String date;

    private List<PaymentAllocation> allocations;

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

    public List<PaymentAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<PaymentAllocation> allocations) {
        this.allocations = allocations;
    }
}
