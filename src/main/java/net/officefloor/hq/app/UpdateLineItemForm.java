package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * The request body for changing a line item on an invoice: which line, and the new description, how
 * many, and the price each. The owning invoice is derived from the line itself.
 */
public class UpdateLineItemForm {

    private Long id;
    private String description;
    private Integer qty;
    private BigDecimal unitPrice;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
