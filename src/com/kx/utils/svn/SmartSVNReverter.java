package com.kx.utils.svn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class SmartSVNReverter {

	private static final String OURCOMMIT = "#Reverting#";
	private static final String MARKERFILE = "MarkerFile.txt";
	private static final int DEFAULT_LIMIT = 20;
	private static final Logger logger = Logger.getLogger(SmartSVNReverter.class.getName());
	private static Config gConfig;
	private static List<String> LOG_COMMAND = new ArrayList(Arrays.asList(new String[] {"svn", "log", "--stop-on-copy"}));
	private static List<String> REVERT_COMMAND = new ArrayList(Arrays.asList(new String[] {"svn", "merge", "-c"}));
	private static List<String> COMMIT_COMMAND = new ArrayList(Arrays.asList(new String[] {"svn", "commit", "-m"}));
	private static List<String> SVN_ADD = new ArrayList(Arrays.asList(new String[] {"svn", "add", MARKERFILE}));

	public static void main(String[] args) throws ParseException, IOException {

		if (!processArgs(args)) {
			return;
		}

		svnUpdate();

		List<String> commitsToRevert = getCommitsToRevert();
		
		boolean firstRun = addMarkerIfNeeded();

		revertInLocal(commitsToRevert, firstRun);

		commitReverts(commitsToRevert);

	}

	@SuppressWarnings("resource")
	private static boolean addMarkerIfNeeded() throws IOException {
		String markerFilePath = gConfig.getWorkingDir() + File.separator + MARKERFILE;
		boolean notExists = !new File(markerFilePath).exists();
		new FileOutputStream(markerFilePath, true).close();
		BufferedWriter bw = null;
		try {
                        bw = new BufferedWriter(new FileWriter(markerFilePath, true));
			bw.write("\nRan @" + new Date());
			bw.flush();
		} finally {
                    bw.close();
		}
		getProcessResults(SVN_ADD);
		return notExists;
	}

	private static String svnUpdate() throws IOException {
		return getProcessResults(new ArrayList(Arrays.asList(new String[] {"svn", "update", "."})));
	}

	private static void commitReverts(List<String> commitsToRevert) throws IOException {
		COMMIT_COMMAND.add("\"" + OURCOMMIT + " - " + commitsToRevert + "\"");
		getProcessResults(COMMIT_COMMAND);
	}

	private static void revertInLocal(List<String> aCommitsToRevert, boolean aFirstRun) throws IOException {
		logger.log(Level.INFO, "Will revert - " + aCommitsToRevert);
		int count = aFirstRun ? aCommitsToRevert.size() - 1 : aCommitsToRevert.size();
		for (int i = 0; i < count; i++) {
			List<String> tempList = new ArrayList<String>(REVERT_COMMAND);
			tempList.add("-" + aCommitsToRevert.get(i));
			tempList.add(".");
			String result = getProcessResults(tempList);
			logger.log(Level.INFO, "Reverted - " + aCommitsToRevert.get(i) + ": \n" + result);
		}
	}

	private static List<String> getCommitsToRevert() throws IOException {
		String result = getProcessResults(LOG_COMMAND);
		return parseRevisionList(result);
	}

	private static List<String> parseRevisionList(String aResult) throws IOException {
		List<String> revisionList = new ArrayList<String>();
		String[] rows = aResult.split(System.getProperty("line.separator"));
		boolean addCommits = false;
		for (int i = 0; i < rows.length; i++) {
			if (addCommits && 
					rows[i].startsWith("r")) {
				revisionList.add(rows[i].split("\\|")[0].trim());
			}
			if (rows[i].contains(OURCOMMIT)) {
				if (addCommits) {
					revisionList.remove(revisionList.size() - 1);
					break;
				} else {
					addCommits = true;
				}
			}
		}
		return revisionList;
	}

	private static String getProcessResults(List<String> aCommand) throws IOException {
		logger.log(Level.INFO, "Running command - " + aCommand);
		aCommand.addAll(gConfig.getUserNamePasswordCommand());
		ProcessBuilder pbuilder = new ProcessBuilder(aCommand);
		pbuilder.redirectErrorStream();
		pbuilder.directory(new File(gConfig.getWorkingDir()));
		Process logProcess = pbuilder.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		logger.log(Level.INFO, "Result : " + builder.toString());
		return builder.toString();
	}

	private static boolean processArgs(String[] args) {
		try {
			setupConfig(args);
		} catch (ParseException e) {
			logCommandsIssue(e.getMessage());
			return false;
		}
		return true;
	}

	private static void logCommandsIssue(String message) {
		logger.log(Level.INFO, "Usage: \n -w <working path where SVN repo is checked out, Optional> -u <UserName> -p <Password>"
				+ "\n For e.g. java -jar LastDaySVNReverter -w /home/kx/SVNCheckout -u sabir -p password");
		logger.log(Level.INFO, "Error - " + message);
	}

	private static void setupConfig(String[] aArgs) throws ParseException {

		Options options = new Options();
		options.addOption(Option.builder("w").argName("WorkingDirectory").hasArg(true).desc("Working Directory (optional)").build());
		options.addOption(Option.builder("u").argName("UserName").required().hasArg(true).desc("Username to login in SVN").build());
		options.addOption(Option.builder("p").argName("Password").required().hasArg(true).desc("password to login in SVN").build());

		DefaultParser parser = new DefaultParser();
		CommandLine commands = parser.parse(options, aArgs);

		gConfig =
				new Config(commands.getOptionValue("u"), commands.getOptionValue("p"), commands.getOptionValue("w", System.getProperty("user.dir")));

	}

	private static class Config {

		private String mWorkingDir;
		private List<String> mUserPassWordString = new ArrayList<String>();

		public Config(String aUsername, String aPassword, String aWorkingDir) {
			super();
			mWorkingDir = aWorkingDir;
			mUserPassWordString.add("--username");
			mUserPassWordString.add(aUsername);
			mUserPassWordString.add("--password");
			mUserPassWordString.add(aPassword);
		}

		public List<String> getUserNamePasswordCommand() {
			return mUserPassWordString;
		}

		public String getWorkingDir() {
			return mWorkingDir;
		}

	}

}
