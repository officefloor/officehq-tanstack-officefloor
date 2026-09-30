package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/lineitems}: the fields the user supplies when adding a line to an
 * invoice — the id of the invoice it belongs to, a description of what is being charged for, how many
 * (qty) and the price of each (unit price). Bound from the JSON payload by the Spring MVC
 * {@code @RequestBody} argument resolver.
 */
public class NewLineItem {

    private Long invoiceId;

    private String description;

    private Integer qty;

    private String unit;

    private BigDecimal unitPrice;

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
