package net.officefloor.hq.app;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single thing being charged for on an invoice: a {@code description}, how many ({@code qty}) and
 * the price of each ({@code unitPrice}). Persisted to the {@code invoice_line_items} table (Flyway
 * V13) and belonging to one invoice ({@code invoiceId} -&gt; invoices.id). The line's own amount is
 * {@code qty * unitPrice}; an invoice's amount is the sum of its lines' amounts, worked out on the
 * server rather than typed. The id is database-generated (IDENTITY) on create; the seed path inserts
 * explicit ids directly via JdbcTemplate (see {@link TestSupportController}).
 */
@Entity
@Table(name = "invoice_line_items")
public class LineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    private String description;

    private Integer qty;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    public LineItem() {
    }

    public LineItem(Long invoiceId, String description, Integer qty, BigDecimal unitPrice) {
        this.invoiceId = invoiceId;
        this.description = description;
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    /** This line's own amount: how many times the price of each. */
    public BigDecimal getAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(qty));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
