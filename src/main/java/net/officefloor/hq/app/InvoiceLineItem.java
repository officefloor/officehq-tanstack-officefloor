package net.officefloor.hq.app;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One line of an invoice: a thing the user is charging for — a description, how many, and the price
 * each. Maps the {@code invoice_line_items} table (Flyway V15__invoice_line_items.sql); the id is
 * IDENTITY-generated on create. The unit price is a {@link BigDecimal} because it is money (fixed
 * scale, no float drift). A line's own amount is its quantity times its unit price, and an invoice's
 * amount is the sum of its lines' amounts.
 */
@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String unit;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    protected InvoiceLineItem() {
    }

    public InvoiceLineItem(Long invoiceId, String description, int quantity, String unit,
            BigDecimal unitPrice) {
        this.invoiceId = invoiceId;
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
    }

    /** Change what this line is for: a new description, how many, the unit, and the price each. */
    public void update(String description, int quantity, String unit, BigDecimal unitPrice) {
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
    }

    public Long getId() {
        return id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /** This line's amount: how many times the price each. */
    public BigDecimal getAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
