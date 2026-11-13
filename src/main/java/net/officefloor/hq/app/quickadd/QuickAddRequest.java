package net.officefloor.hq.app.quickadd;

import jakarta.validation.constraints.Size;

/** The quick-add bar's fields: a person's name (required) and a claim's amount (required). */
public record QuickAddRequest(@Size(max = 200) String name, @Size(max = 30) String amount) {
}
