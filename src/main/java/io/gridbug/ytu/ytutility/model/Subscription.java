
package io.gridbug.ytu.ytutility.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.joda.time.DateTime;

@Entity
@Table(name="subscriptions") 
public class Subscription {

    @Id
    @GeneratedValue
    private long id;

    /** 
     * api field: snippet.title
     */
    private String name;

    /**
     * api field: snippet.description
     */
    @Column(name="description", length=10240)
    private String description;

    /**
     * api field: snippet.resourceid.channelid
     */
    private String ytId;

    /**
     * api field: snippet.publishedAt
     */
    private DateTime subscribedOn;

    /**
     * not from api; this is the date and time of the last check for new videos
     */
    private DateTime lastCheck;


    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getYtId() { return ytId; }
    public void setYtId(String ytId) { this.ytId = ytId; }

    public DateTime getSubscribedOn() { return subscribedOn; }
    public void setSubscribedOn(DateTime subscribedOn) { this.subscribedOn = subscribedOn; }

    public DateTime getLastCheck() { return lastCheck; }
    public void setLastCheck(DateTime lastCheck) { this.lastCheck = lastCheck; }
}