/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.utilities.Environment;
import chs.utilities.LogFileFilter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

/**
 * This class returns the log file path
 */
public class TCMbseLogFileFilter
{

	private String logFileFullPath = "";
	private String logFileName = "";

	private void getLogFileFullPath()
	{
		try {
			logFileName = LogFileFilter.getCurrentLogFile();
			String folder = Environment.getTemp();
			logFileFullPath = Path.of(folder, logFileName).toUri().toString();
			System.out.println("log file full path " + logFileFullPath);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@NotNull
	protected String getLinkedLogFile()
	{
		getLogFileFullPath();
		return getLinked();
	}

	@NotNull
	protected String getLinked()
	{
		return "<a href=\"" + logFileFullPath + "\">" + logFileName + "</a>";
	}
}