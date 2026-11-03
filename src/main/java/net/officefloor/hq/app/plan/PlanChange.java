package net.officefloor.hq.app.plan;

/** A change of a plan's amount: the day it was made (yyyy-mm-dd) and the new amount (two decimals). */
public record PlanChange(String on, String amount) {
}
