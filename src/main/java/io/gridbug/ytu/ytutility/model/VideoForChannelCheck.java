package io.gridbug.ytu.ytutility.model;

import org.joda.time.DateTime;

public class VideoForChannelCheck {

    private String channelId;
    private DateTime requestedOn;
    private DateTime performedOn;
    private boolean outcome;
    private String outcomeMessage;

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public DateTime getRequestedOn() { return requestedOn; }
    public void setRequestedOn(DateTime requestedOn) { this.requestedOn = requestedOn; }

    public DateTime getPerformedOn() { return performedOn; }
    public void setPerformedOn(DateTime performedOn) { this.performedOn = performedOn; }

    public boolean getOutcome() { return outcome; }
    public void setOutcome(boolean outcome) { this.outcome = outcome; }

    public String getOutcomeMessage() { return outcomeMessage; }
    public void setOutcomeMessage(String outcomeMessage) { this.outcomeMessage = outcomeMessage; }
}