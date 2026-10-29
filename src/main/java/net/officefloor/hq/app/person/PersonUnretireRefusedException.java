package net.officefloor.hq.app.person;

/** Only a retired person can be unretired. */
public class PersonUnretireRefusedException extends Exception {

    public PersonUnretireRefusedException(String message) {
        super(message);
    }
}
