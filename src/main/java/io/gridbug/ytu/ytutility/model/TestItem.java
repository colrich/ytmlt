package io.gridbug.ytu.ytutility.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.joda.time.DateTime;

@Entity
@Table(name="testitems")
public class TestItem {


    @GeneratedValue
    @Id
    private long id;

    private DateTime creation;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public DateTime getCreation() { return creation; }
    public void setCreation(DateTime creation) { this.creation = creation; }
}