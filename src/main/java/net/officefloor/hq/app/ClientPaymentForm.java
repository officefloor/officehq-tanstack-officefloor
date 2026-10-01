package net.officefloor.hq.app;

import java.math.BigDecimal;
import java.util.List;

/**
 * The request body for recording a lump payment a client made, split across several of their open
 * invoices: which client paid, the date it was paid (an ISO yyyy-MM-dd string), the total amount,
 * and how that total is allocated across invoices (one {@link PaymentAllocation} per invoice it is
 * spread over). The split lands as one payment row per allocation, so each invoice's balance then
 * reflects its own share.
 */
public class ClientPaymentForm {

    private Long clientId;
    private BigDecimal amount;
    private String date;
    private List<PaymentAllocation> allocations;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
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

    public List<PaymentAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<PaymentAllocation> allocations) {
        this.allocations = allocations;
    }
}
