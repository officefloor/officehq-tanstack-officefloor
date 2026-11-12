package net.officefloor.hq.app.person;

/** Body of a hold, as typed: its from-date (blank for the sheet's today), its to-date (blank for no
 *  end) and who puts the person on it. */
public record HoldRequest(String from, String to, String user) {
}
