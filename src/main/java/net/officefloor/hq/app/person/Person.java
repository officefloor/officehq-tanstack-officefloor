package net.officefloor.hq.app.person;

/** A person on a sheet. Mutable so an Apply step can change it in place. */
public class Person {

    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    private int version;

    public Person(long id, String sheetId, String name, String grade) {
        this(id, sheetId, name, grade, 0);
    }

    public Person(long id, String sheetId, String name, String grade, int version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public String getGrade() { return grade; }
    public int getVersion() { return version; }
    public void setName(String name) { this.name = name; }
    public void setGrade(String grade) { this.grade = grade; }
    public void setVersion(int version) { this.version = version; }
}
