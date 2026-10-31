package net.officefloor.hq.app.rollup;

/** Who makes a change, published by the change's validating step for the steps after it. */
public record Actor(String user) {
}
