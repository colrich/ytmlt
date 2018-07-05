package io.gridbug.ytu.ytutility;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.WriterBasedJsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonParser;

import com.google.api.services.youtube.model.*;
import com.google.api.services.youtube.YouTube;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.Transactional;

import io.gridbug.ytu.ytutility.configuration.YTUProperties;
import io.gridbug.ytu.ytutility.configuration.YoutubeService;
import io.gridbug.ytu.ytutility.dao.ChannelInfoRepository;
import io.gridbug.ytu.ytutility.dao.SubscriptionRepository;
import io.gridbug.ytu.ytutility.model.Subscription;
import io.gridbug.ytu.ytutility.model.VideoForChannelCheck;
import io.gridbug.ytu.ytutility.model.ChannelCheck;
import io.gridbug.ytu.ytutility.model.ChannelInfo;


@SpringBootApplication
public class YtUtilityApplication implements CommandLineRunner {

	private static final Logger LOGGER = Logger.getLogger(YtUtilityApplication.class.getName());

	public static void main(String[] args) {
		SpringApplication.run(YtUtilityApplication.class, args);
	}

	private static final int NORMAL_EXIT = 0;
	private static final int PATH_FAILURE = 1;

	private JsonFactory jsonFactory;
	
	@Autowired
	private YTUProperties ytProperties;

	@Autowired
	private ApplicationArguments pargs;

	@Autowired
	private YoutubeService ytService;

	@Autowired
	private SubscriptionRepository subsdao;

	@Autowired
	private ChannelInfoRepository chandao;

	public void run(String... args) {
		try {
			LOGGER.log(Level.INFO, "yt utility CommandLineRunner invoked...");

			if (!ensurePaths()) {
				LOGGER.log(Level.SEVERE, "yt utility unable to create/ensure file paths! unrecoverable, exiting");
				System.exit(PATH_FAILURE);
			}

			YouTube youtube = ytService.getYouTubeService();
//			printChannelStats(youtube, "PowerfulJRE");
//			writeUserSubs(youtube);
//			writeChannelInfo(youtube, "UCThv5tYUVaG4ZPA3p6EXZbQ");
//			writeChannelInfo(youtube, "UC6E2mP01ZLH_kbAyeazCNdg");
//			writeChannelUploads(youtube, "UU6E2mP01ZLH_kbAyeazCNdg");
//			writeVideoDetails(youtube, "Ob8gpcBmGvg");
//			writeVideoComments(youtube, "Ob8gpcBmGvg");
//			loadChannelInfoJson("upv.json");
//			testDatabase();

			// if we're invoked with no args, we don't do any CLI tasks. in this case, we simply
			// want the spring mvc webapp to start serving, so we return from this CLI runner 
			// and allow that to happen
			if (pargs.getOptionNames().size() == 0) {
				LOGGER.log(Level.INFO, "yt utility started with no args; running web service");
				return;
			}
			
			// if invoked with --fetch-subs, we will refresh the jsons in the subs json directory
			// by pulling all the pages from the api
			if (pargs.getOptionNames().contains("fetch-subs")) {
				LOGGER.log(Level.INFO, "yt utility called fetch-subs");
				writeUserSubs(youtube, ytProperties.getSubsPath());
			}

			// if invoked with --subs-to-db, we'll create/update entries in the database for each
			// subscription in each of the files in the subs json directory
			if (pargs.getOptionNames().contains("subs-to-db")) {
				LOGGER.log(Level.INFO, "yt utility called subs-to-db");
				try (Stream<Path> paths = Files.walk(Paths.get(ytProperties.getSubsPath()))) {
					LOGGER.log(Level.INFO, "results: " + paths.filter(Files::isRegularFile)
							.filter(path -> path.getFileName().toString().startsWith("mysubs-"))
							.map(path -> putSubJsonToDB(path))
							.collect(Collectors.toList()));
				}
			}

			// if invoked with --stage-channel-check, we write channel check descriptors to the 
			// channel check directory. if no entry exists in the db, we will write the descriptor;
			// if an entry exists, we write the descriptor if last check was > specified threshold
			if (pargs.getOptionNames().contains("stage-channel-check")) {
				LOGGER.log(Level.INFO, "yt utility called stage-channel-check");
				stageChannelCheck(pargs);
			}

			// if invoked with --run-channel-check, we will look at the descriptors in the channel
			// check directory and pull from the api and update the database. the descriptors are 
			// removed as they are processed
			if (pargs.getOptionNames().contains("run-channel-check")) {
				LOGGER.log(Level.INFO, "yt utility called run-channel-check");
				runChannelCheck(youtube, pargs);
			}

			// if invoked with --stage-video-for-channel-check, we will write a descriptor requesting
			// a check for new videos for each channel
			if (pargs.getOptionNames().contains("stage-video-for-channel-check")) {
				LOGGER.log(Level.INFO, "yt utility called stage-video-for-channel-check");
				stageVideoForChannelCheck();
			}

			// if invoked with --run-video-for-channel-check, we will look at the descriptors in the
			// check directory and pull uploads from the channel via api. we'll do a full scrape if
			// no existing info for the channel exists; otherwise we'll pull only new video entries
			if (pargs.getOptionNames().contains("run-video-for-channel-check")) {
				LOGGER.log(Level.INFO, "yt utility called run-video-for-channel-check");
				runVideoForChannelCheck(youtube);
			}

			// if invoked with --fetch-video-details, we use the api to get detailed info about each
			// video in the channel data directory, skipping those that have already been fetched
			if (pargs.getOptionNames().contains("fetch-video-details")) {
				LOGGER.log(Level.INFO, "yt utility called fetch-video-details");
				fetchVideoDetails(youtube);
			}



			LOGGER.log(Level.INFO, "yt utility CommandLineRunner execution complete; exiting via normal path");
			System.exit(NORMAL_EXIT);
		}
		catch (IOException ioe) {
			System.out.println("failed to get youtube service");
			ioe.printStackTrace();
		}
	}

	private void fetchVideoDetails(YouTube youtube) throws IOException {
		try (Stream<Path> channels = Files.walk(Paths.get(ytProperties.getChannelDataPath()))) {
			List<Boolean> outcomes = channels.filter(Files::isRegularFile)
				.filter(path -> !path.toString().contains("details-"))
				.map(channelDirectory -> {
					try {
						LOGGER.log(Level.INFO, "fetch-video-details -> each-channel | on path: " + channelDirectory);

						// read the video descriptor to get necessary info for the api call
						JsonParser parser = ytService.getJsonFactory().createJsonParser(new FileInputStream(channelDirectory.toFile()));
						PlaylistItem check = parser.parse(PlaylistItem.class);
						
						// check to see if we already have the details file, don't refetch if so
						File excheck = new File(getChannelDataDirectory(check.getSnippet().getChannelId()) + "details-" + check.getSnippet().getResourceId().getVideoId() + ".json");
						if (excheck.exists()) {
							LOGGER.log(Level.INFO, "details file exists: " + excheck.getAbsolutePath() + ", skipping");
							return true;
						}

						// call the api to get the video details
						YouTube.Videos.List video = youtube.videos().list("snippet,contentDetails,status,statistics");
						video.setId(check.getSnippet().getResourceId().getVideoId());
						VideoListResponse response = video.execute();

						// write the details file with the api results
						writeJsonGObject(response.getItems().get(0), getChannelDataDirectory(check.getSnippet().getChannelId()) + "details-" + check.getSnippet().getResourceId().getVideoId() + ".json");

						return true;
					}
					catch (Exception ioe) {
						LOGGER.log(Level.INFO, "fetch-video-details | io exception: " + channelDirectory, ioe);
						return false;
					}
				})
				.collect(Collectors.toList());
			LOGGER.log(Level.INFO, "fetch-video-details | outcomes: " + outcomes);
		}
	}

	private void runVideoForChannelCheck(YouTube youtube) throws IOException {
		try (Stream<Path> paths = Files.walk(Paths.get(ytProperties.getVideoForChannelCheckPath()))) {
			List<Boolean> outcomes = paths.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".json"))
				.map(path -> {
					try {
						LOGGER.log(Level.INFO, "run-video-for-channel-check | operating on path: " + path);
						com.fasterxml.jackson.core.JsonParser parser = getJsonFactory().createParser(path.toFile());
						VideoForChannelCheck check = parser.readValueAs(VideoForChannelCheck.class);
						LOGGER.log(Level.INFO, "run-video-for-channel-check | running channel check: " +
							check.getChannelId());

						Optional<ChannelInfo> chan = chandao.findById(check.getChannelId());
						if (chan.isPresent()) {
							ensureDirectory(getChannelDataDirectory(check.getChannelId()));
							
							YouTube.PlaylistItems.List videos = youtube.playlistItems().list("snippet,contentDetails");
							videos.setPlaylistId(chan.get().getUploadsPlaylistId());
							videos.setMaxResults(50L);
							PlaylistItemListResponse response = videos.execute();
							response.getItems().forEach(video -> {
								try {
									LOGGER.log(Level.INFO, "run-video-for-channel-check -> for-each-video | writing video entry: " + video.getSnippet().getResourceId().getVideoId());
									// write a json file with each video item in the channel's video data directory
									writeJsonGObject(video, getChannelDataDirectory(check.getChannelId()) + 
										video.getSnippet().getResourceId().getVideoId() + ".json");
								}
								catch (IOException ioe) {
									LOGGER.log(Level.INFO, "run-video-for-channel-check -> for-each-video | io exception: " + video, ioe);
								}
							});
						}
						else {
							LOGGER.log(Level.INFO, "run-video-for-channel-check | no channel info found for: " + 
								check.getChannelId());
							return false;
						}

						LOGGER.log(Level.INFO, "run-video-for-channel-check | about to delete descriptor: " + path);
						path.toFile().delete();

						delaySeconds(2);
						return true;
					}
					catch (IOException ioe) {
						LOGGER.log(Level.INFO, "run-video-for-channel-check | io exception on path: " + path, ioe);
						return false;
					}
				})
				.collect(Collectors.toList());
			LOGGER.log(Level.INFO, "run-video-for-channel-check | outcomes: " + outcomes);
		}
		
	}

	private String getChannelDataDirectory(String channelId) {
		return ytProperties.getChannelDataPath() + File.separator + channelId + File.separator;
	}

	private void ensureDirectory(String path) {
		File dir = new File(path);
		if (!dir.exists()) dir.mkdirs();
	}

	private void stageVideoForChannelCheck() {
		Iterable<ChannelInfo> chans = chandao.findAll();
		chans.forEach(chan -> {
			try {
				LOGGER.log(Level.INFO, "stage-video-check | writing descriptor for " + chan.getId());
				writeVideoForChannelCheckDescriptor(chan.getId());
			}
			catch (IOException ioe) {
				LOGGER.log(Level.INFO, "stage-video-check | io exception writing descriptor for channel: " +
						chan.getId(), ioe);
			}
		});
	}

	private void runChannelCheck(YouTube youtube, ApplicationArguments pargs) throws IOException {
		try (Stream<Path> paths = Files.walk(Paths.get(ytProperties.getChannelCheckPath()))) {
			List<Boolean> outcomes = paths.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".json"))
				.map(path -> {
					try {
						// first we read the descriptor to find the id and any other necessary data
						LOGGER.log(Level.INFO, "run-channel-check | operating on path: " + path);
						com.fasterxml.jackson.core.JsonParser parser = getJsonFactory().createParser(path.toFile());
						ChannelCheck check = parser.readValueAs(ChannelCheck.class);
						LOGGER.log(Level.INFO, "run-channel-check | running channel check: " + check.getId());
						
						// call the api and get the result
						YouTube.Channels.List videos = youtube.channels().list("snippet,contentDetails,statistics");
						videos.setId(check.getId());
						ChannelListResponse response = videos.execute();
						LOGGER.log(Level.INFO, "run-channel-check | api responds # records: " + 
							response.getPageInfo().getTotalResults());

						// create the model object for our db and save it
						ChannelInfo chan = new ChannelInfo();
						chan.setId(check.getId());
						chan.setCustomUrl(response.getItems().get(0).getSnippet().getCustomUrl());
						chan.setUploadsPlaylistId(response.getItems().get(0).getContentDetails().getRelatedPlaylists().getUploads());
						chan.setCreatedOn(new DateTime(
								response.getItems().get(0).getSnippet().getPublishedAt().getValue()));
						chan.setDescription(response.getItems().get(0).getSnippet().getDescription());
						chan.setLastCheck(DateTime.now());
						chan.setName(response.getItems().get(0).getSnippet().getTitle());
						chan.setThumbnailUrl(
								response.getItems().get(0).getSnippet().getThumbnails().getDefault().getUrl());
						chan.setSubscriberCount(
								response.getItems().get(0).getStatistics().getSubscriberCount().intValue());
						chan.setVideoCount(response.getItems().get(0).getStatistics().getVideoCount().intValue());
						chan.setViewCount(response.getItems().get(0).getStatistics().getViewCount().longValue());
						chandao.save(chan);
						LOGGER.log(Level.INFO, "run-channel-check | saved channel info to db");

						// fill out the rest of the descriptor and save it into the completed directory; 
						check.setOutcome(true);
						check.setOutcomeMessage("success");
						check.setPerformedOn(DateTime.now());
						writeCompletedChannelCheckDescriptor(check);
						LOGGER.log(Level.INFO, "run-channel-check | wrote descriptor in completed directory");

						// finally, delete the original check descriptor
						path.toFile().delete();
						LOGGER.log(Level.INFO, "run-channel-check | deleted original check descriptor");

						// api-friendly delay
						delaySeconds(2);
						return true;
					}
					catch (IOException ioe) {
						LOGGER.log(Level.WARNING, "run-channel-check | IOException on path: " + path, ioe);
						return false;
					}
				})
				.collect(Collectors.toList());
			LOGGER.log(Level.INFO, "run-channel-check | outcomes: " + outcomes);
		}		
	}

	/**
	 * goes over the subs in the db and writes out a channel-check descriptor for the ones that haven't
	 * been checked during the specified threshold
	 */
	private void stageChannelCheck(ApplicationArguments pargs) {
		Duration checkThreshold = getCheckThreshold(pargs);

		Iterable<Subscription> allsubs = subsdao.findAll();
		allsubs.forEach(item -> {
			try {
				// first check to see if we have an entry for this channel already
				Optional<ChannelInfo> chan = chandao.findById(item.getYtId());
				if (chan.isPresent()) {
					// if so, is the last check older than the threshold?
					if (chan.get().getLastCheck().isBefore(DateTime.now().minus(checkThreshold))) {
						// it is - write the descriptor
						LOGGER.log(Level.INFO, "stage-channel-check | channel " + chan.get().getId() + 
							" due for channel check, writing descriptor");
						writeChannelCheckDescriptor(chan.get().getId());
					}
					else {
						// it's not - no action except log
						LOGGER.log(Level.INFO, "stage-channel-check | channel " + chan.get().getId() + 
							" not due for channel check");
					}
				}
				else {
					// no entry for this channel; write descriptor
					LOGGER.log(Level.INFO, "stage-channel-check | channel " + item.getYtId() + 
						" has no channel info, writing descriptor");
					writeChannelCheckDescriptor(item.getYtId());
				}
			}
			catch (IOException ioe) {
				LOGGER.log(Level.INFO, "stage-channel-check | IO exception on item " + item.getYtId(), ioe);
			}
		});
	}

	/**
	 * writes the channel-check json descriptor that gets picked up by the channel checker
	 */
	private void writeChannelCheckDescriptor(String channelId) throws IOException {
		ChannelCheck check = new ChannelCheck();
		check.setId(channelId);
		check.setRequestedOn(DateTime.now());
		writeJsonDescriptor(check, ytProperties.getChannelCheckPath() + File.separator + channelId + ".json");
	}

	private void writeCompletedChannelCheckDescriptor(ChannelCheck check) throws IOException {
		writeJsonDescriptor(check, ytProperties.getCompletedActionsPath() + File.separator 
			+ "channel-check-" + check.getId() + ".json");
	}

	/**
	 * writes the channel-check json descriptor that gets picked up by the channel checker
	 */
	private void writeVideoForChannelCheckDescriptor(String channelId) throws IOException {
		VideoForChannelCheck check = new VideoForChannelCheck();
		check.setChannelId(channelId);
		check.setRequestedOn(DateTime.now());
		writeJsonDescriptor(check, ytProperties.getVideoForChannelCheckPath() + File.separator + channelId + ".json");
	}

	private void writeCompletedVideoForChannelCheckDescriptor(VideoForChannelCheck check) throws IOException {
		writeJsonDescriptor(check, ytProperties.getCompletedActionsPath() + File.separator 
			+ "video-for-channel-check-" + check.getChannelId() + ".json");
	}


	/**
	 * takes a json file containing one or more subscription records from the yt api and makes the
	 * tracking db entries, updating existing records if found
	 */
	private boolean putSubJsonToDB(Path filePath) {
		try {
			JsonParser parser = ytService.getJsonFactory().createJsonParser(Files.newInputStream(filePath));
			SubscriptionListResponse response = parser.parse(SubscriptionListResponse.class);
			response.getItems().forEach(item -> {
				LOGGER.log(Level.INFO, "putSubJsonToDB| dealing with " + item.getSnippet().getResourceId().getChannelId());
				Subscription sub = new Subscription();

				// check to see if we have this item in the db yet - if so, we just want to update it, otherwise create
				Optional<Subscription> existingSub = subsdao.findByYtId(item.getSnippet().getResourceId().getChannelId());
				if (existingSub.isPresent()) sub.setId(existingSub.get().getId());

				// set the rest of the properties
				sub.setYtId(item.getSnippet().getResourceId().getChannelId());
				sub.setName(item.getSnippet().getTitle());
				sub.setDescription(item.getSnippet().getDescription());
				sub.setSubscribedOn(new DateTime(item.getSnippet().getPublishedAt().getValue()));
				sub.setLastCheck(new DateTime());

				// save to db
				subsdao.save(sub);
			});
	
			LOGGER.log(Level.INFO, "putSubJsonToDB | " + filePath + " | successful");
			return true;
		}
		catch (IOException ioe) {
			LOGGER.log(Level.WARNING, "putSubJsonToDB | " + filePath + " | failed with IO exception", ioe);
			return false;
		}
	}


	/**
	 * takes the properties info and makes the necessary paths in case they don't exist
	 * 
	 * @return true if paths already exist or were successfully created, false otherwise
	 */
	private boolean ensurePaths() {
		LOGGER.log(Level.INFO, "ytp jsonpath: " + ytProperties.getJsonPath());
		File path = new File(ytProperties.getJsonPath());
		if (!path.exists()) path.mkdirs();

		File subs = new File(ytProperties.getSubsPath());
		if (!subs.exists()) subs.mkdirs();
		
		File videos = new File(ytProperties.getVideosPath());
		if (!videos.exists()) videos.mkdirs();

		File subcheck = new File(ytProperties.getSubcheckPath());
		if (!subcheck.exists()) subcheck.mkdirs();

		File chancheck = new File(ytProperties.getChannelCheckPath());
		if (!chancheck.exists()) chancheck.mkdirs();

		File v4chancheck = new File(ytProperties.getVideoForChannelCheckPath());
		if (!v4chancheck.exists()) v4chancheck.mkdirs();

		File completed = new File(ytProperties.getCompletedActionsPath());
		if (!completed.exists()) completed.mkdirs();

		File channeldata = new File(ytProperties.getChannelDataPath());
		if (!channeldata.exists()) channeldata.mkdirs();

		return true;
	}

	private Duration getCheckThreshold(ApplicationArguments pargs) {
		int minsThreshold;
		if (pargs.getOptionNames().contains("last-check-gt-minutes")) {
			minsThreshold = Integer.parseInt(pargs.getOptionValues("last-check-gt-minutes").get(0));
		}
		else minsThreshold = 60 * 24;

		Duration checkMins = new Duration(minsThreshold*60*1000);
		LOGGER.log(Level.INFO, "getCheckThreshold| last check greater than threshold " + minsThreshold +
				" minutes / " + checkMins + " millis");
		return checkMins;
	}

	private JsonFactory getJsonFactory() {
		if (jsonFactory == null) {
			jsonFactory = new JsonFactory();
			ObjectMapper mapper = new ObjectMapper();
			mapper.findAndRegisterModules();
			jsonFactory.setCodec(mapper);
		}
		return jsonFactory;
	}

	private void writeJsonDescriptor(Object descriptor, String filename) throws IOException {
		JsonGenerator jsongen = getJsonFactory().createGenerator(new FileOutputStream(filename));
		jsongen.writeObject(descriptor);
		jsongen.close();
	}

	private void writeJsonGObject(Object googleObject, String filename) throws IOException {
		com.google.api.client.json.JsonGenerator gen = ytService.getJsonFactory().createJsonGenerator(new FileWriter(new File(filename)));
		gen.serialize(googleObject);
		gen.close();
//		JsonGenerator jsongen = getJsonFactory().createGenerator(new FileOutputStream(filename));
//		jsongen.writeObject(descriptor);
//		jsongen.close();
	}

	private void delaySeconds(int seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		}
		catch (InterruptedException ie) {
			LOGGER.log(Level.INFO, "delay interrupted", ie);
		}
	}
	

	private void writeVideoComments(YouTube youtube, String videoId) throws IOException {
		YouTube.CommentThreads.List comments = youtube.commentThreads().list("snippet,replies");
		comments.setVideoId(videoId);
		comments.setTextFormat("plainText");
		CommentThreadListResponse response = comments.execute();
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("comments.json"));
		writer.write(response.toString());
		writer.close();
	}

	private void writeVideoDetails(YouTube youtube, String videoId) throws IOException {
		YouTube.Videos.List video = youtube.videos().list("snippet,contentDetails,status,statistics");
		video.setId(videoId);
		VideoListResponse response = video.execute();
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("vid.json"));
		writer.write(response.toString());
		writer.close();
	}

	private void writeChannelUploads(YouTube youtube, String playlistId) throws IOException {
		YouTube.PlaylistItems.List videos = youtube.playlistItems().list("snippet,contentDetails");
		videos.setPlaylistId(playlistId);
		PlaylistItemListResponse response = videos.execute();
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("videos.json"));
		writer.write(response.toString());
		writer.close();
	}

	private void writeChannelInfo(YouTube youtube, String channelId) throws IOException {
		YouTube.Channels.List videos = youtube.channels().list("snippet,contentDetails,statistics");
		videos.setId(channelId);
		ChannelListResponse response = videos.execute();
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("upvx.json"));
		writer.write(response.toString());
		writer.close();
	}

	private void loadChannelInfoJson(String jsonpath) throws IOException {
		JsonParser parser = ytService.getJsonFactory().createJsonParser(new FileInputStream(new File(jsonpath)));
		ChannelListResponse response = parser.parse(ChannelListResponse.class);
		System.out.println("found in response: " + response.getItems().get(0).getSnippet().getDescription());
	}

	private void writeUserSubs(YouTube youtube, String path) throws IOException {
		String nextPageToken = null;
		do {
			YouTube.Subscriptions.List mysubs = youtube.subscriptions().list("snippet,contentDetails");
			mysubs.setMine(true);
			if (nextPageToken != null) mysubs.setPageToken(nextPageToken);

			LOGGER.log(Level.INFO, "writeUserSubs | getting next page: " + nextPageToken);
			OutputStreamWriter writer = new OutputStreamWriter(
					new FileOutputStream(path+File.separator+"mysubs-"+nextPageToken+".json"));
			SubscriptionListResponse response = mysubs.execute();
			writer.write(response.toString());
			writer.close();

			nextPageToken = response.getNextPageToken();
		}
		while (nextPageToken != null && !nextPageToken.equals(""));
	}

	private void printChannelStats(YouTube youtube, String name) {
		try {
			YouTube.Channels.List channelsListByUsernameRequest = youtube.channels().list("snippet,contentDetails,statistics");
			channelsListByUsernameRequest.setForUsername(name);

			ChannelListResponse response = channelsListByUsernameRequest.execute();
			Channel channel = response.getItems().get(0);
			System.out.printf(
				"This channel's ID is %s. Its title is '%s', and it has %s views.\n",
				channel.getId(),
				channel.getSnippet().getTitle(),
				channel.getStatistics().getViewCount());
		} catch (GoogleJsonResponseException e) {
			e.printStackTrace();
			System.err.println("There was a service error: " +
				e.getDetails().getCode() + " : " + e.getDetails().getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
		}		
	}
}
