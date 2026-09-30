package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/lineitems/edit}: the id of the line to change plus its new
 * description, quantity and unit price. Bound from the JSON payload by the Spring MVC
 * {@code @RequestBody} argument resolver.
 */
public class EditLineItem {

    private Long id;

    private String description;

    private Integer qty;

    private String unit;

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
