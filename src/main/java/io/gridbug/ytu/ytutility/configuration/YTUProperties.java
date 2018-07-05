package io.gridbug.ytu.ytutility.configuration;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("ytu.core")
public class YTUProperties {

    /**
     * this is the root path for the various json directories
     */
    private String jsonPath;

    public String getJsonPath() { return jsonPath; }
    public void setJsonPath(String jsonPath) { this.jsonPath = jsonPath; }

    private String subsSubpath;

    public String getSubsSubpath() { return subsSubpath; }
    public void setSubsSubpath(String subsSubpath) { this.subsSubpath = subsSubpath; }

    private String videosSubpath;

    public String getVideosSubpath() { return videosSubpath; }
    public void setVideosSubpath(String videosSubpath) { this.videosSubpath = videosSubpath; } 

    private String dataStoreDir;

    public String getDataStoreDir() { return dataStoreDir; }
    public void setDataStoreDir(String dataStoreDir) { this.dataStoreDir = dataStoreDir; }

    private String subcheckSubpath;

    public String getSubcheckSubpath() { return subcheckSubpath; }
    public void setSubcheckSubpath(String subcheckSubpath) { this.subcheckSubpath = subcheckSubpath; }

    private String chancheckSubpath;

    public String getChancheckSubpath() { return chancheckSubpath; }
    public void setChancheckSubpath(String chancheckSubpath) { this.chancheckSubpath = chancheckSubpath; }

    private String videoForChancheckSubpath;
    
    public String getVideoForChancheckSubpath() { return videoForChancheckSubpath; }
    public void setVideoForChancheckSubpath(String videoForChancheckSubpath) { this.videoForChancheckSubpath = videoForChancheckSubpath; }

    private String completedActionsSubpath;

    public String getCompletedActionsSubpath() { return completedActionsSubpath; }
    public void setCompletedActionsSubpath(String completedActionsSubpath) { this.completedActionsSubpath = completedActionsSubpath; }

    private String channelDataSubpath;

    public String getChannelDataSubpath() { return channelDataSubpath; }
    public void setChannelDataSubpath(String channelDataSubpath) { this.channelDataSubpath = channelDataSubpath; }


    public String getSubsPath() {
        return getJsonPath() + File.separator + getSubsSubpath();
    }

    public String getVideosPath() {
        return getJsonPath() + File.separator + getVideosSubpath();
    }

    public String getSubcheckPath() {
        return getJsonPath() + File.separator + getSubcheckSubpath();
    }

    public String getChannelCheckPath() {
        return getJsonPath() + File.separator + getChancheckSubpath();
    }

    public String getVideoForChannelCheckPath() {
        return getJsonPath() + File.separator + getVideoForChancheckSubpath();
    }

    public String getCompletedActionsPath() {
        return getJsonPath() + File.separator + getCompletedActionsSubpath();
    }

    public String getChannelDataPath() {
        return getJsonPath() + File.separator + getChannelDataSubpath();
    }
}