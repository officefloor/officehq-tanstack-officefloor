package net.officefloor.hq.app.rate;

/** Body of a rate import: lines of grade,rate,from,to (the from-date and to-date may be absent or blank). */
public record ImportRatesRequest(String text) {
}
