package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * The request body for adding a line to an invoice: what is being charged for (description), how
 * many (qty) and the price each (unitPrice). The invoice id comes from the path, and the line id is
 * generated. Bound from the POST JSON body via {@code @RequestBody}.
 */
public class NewLineItem {

    private String description;

    private Integer qty;

    private String unit;

    private BigDecimal unitPrice;

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
