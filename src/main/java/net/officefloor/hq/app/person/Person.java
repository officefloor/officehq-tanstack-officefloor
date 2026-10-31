package net.officefloor.hq.app.person;

/** A person on a sheet. `id` is null until the person is first saved. */
public class Person {
    private Long id;
    private final String sheetId;
    private String name;
    private String grade;

    public Person(Long id, String sheetId, String name, String grade) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}
