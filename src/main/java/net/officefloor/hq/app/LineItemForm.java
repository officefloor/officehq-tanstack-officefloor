package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * The request body for adding a line item to an invoice: which invoice, what the line is for, how
 * many, and the price each.
 */
public class LineItemForm {

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
