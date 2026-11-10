package net.officefloor.hq.app.quick;

/** A validated quick-add: the person's trimmed name. The claim itself is published as a {@code ClaimInput}. */
public record QuickAdd(String name) {
}
