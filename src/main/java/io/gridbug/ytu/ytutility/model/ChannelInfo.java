package io.gridbug.ytu.ytutility.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.joda.time.DateTime;

@Entity
@Table(name="channelinfos") 
public class ChannelInfo {

    /**
     * api field: id
     */
    @Id
    private String id;

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
     * api field: snippet.publishedAt
     */
    private DateTime createdOn;

    /** 
     * api field: snippet.customUrl
     */
    private String customUrl;

    /**
     * api field: snippet.thumbnails.[high,medium,default]
     */
    @Column(name="thumbnail_url", length=2048)
    private String thumbnailUrl;
    
    /**
     * api field: statistics.subscriberCount
     */
    private int subscriberCount;

    /**
     * api field: statistics.videoCount
     */
    private int videoCount;
    
    /**
     * api field: statistics.viewCount
     */
    private long viewCount;

    /** 
     * last time this channel was checked via api
     */
    private DateTime lastCheck;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public DateTime getCreatedOn() { return createdOn; }
    public void setCreatedOn(DateTime createdOn) { this.createdOn = createdOn; }

    public String getCustomUrl() { return customUrl; }
    public void setCustomUrl(String customUrl) { this.customUrl = customUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public int getSubscriberCount() { return subscriberCount; }
    public void setSubscriberCount(int subscriberCount) { this.subscriberCount = subscriberCount; }

    public int getVideoCount() { return videoCount; }
    public void setVideoCount(int videoCount) { this.videoCount = videoCount; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public DateTime getLastCheck() { return lastCheck; }
    public void setLastCheck(DateTime lastCheck) { this.lastCheck = lastCheck; }

}