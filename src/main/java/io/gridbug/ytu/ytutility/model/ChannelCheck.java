package io.gridbug.ytu.ytutility.model;

import org.joda.time.DateTime;

public class ChannelCheck {

    private String id;
    private DateTime requestedOn;
    private DateTime performedOn;
    private boolean outcome;
    private String outcomeMessage;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public DateTime getRequestedOn() { return requestedOn; }
    public void setRequestedOn(DateTime requestedOn) { this.requestedOn = requestedOn; }

    public DateTime getPerformedOn() { return performedOn; }
    public void setPerformedOn(DateTime performedOn) { this.performedOn = performedOn; }

    public boolean getOutcome() { return outcome; }
    public void setOutcome(boolean outcome) { this.outcome = outcome; }

    public String getOutcomeMessage() { return outcomeMessage; }
    public void setOutcomeMessage(String outcomeMessage) { this.outcomeMessage = outcomeMessage; }
}