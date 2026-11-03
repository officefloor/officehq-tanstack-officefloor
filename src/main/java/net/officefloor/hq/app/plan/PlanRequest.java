package net.officefloor.hq.app.plan;

public record PlanRequest(String amount, String start, String every, String day, String end, String sheet) {
}
