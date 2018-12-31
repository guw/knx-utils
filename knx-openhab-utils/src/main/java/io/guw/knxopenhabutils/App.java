package io.guw.knxopenhabutils;

import java.io.File;
import java.util.concurrent.Callable;

import io.guw.knxopenhabutils.knxproject.KnxProjectFile;

import picocli.CommandLine;
import picocli.CommandLine.Parameters;

/**
 * Converts a knxproj file to a set of files for openHAB.
 */
public class App implements Callable<Void> {

	public static void main(String[] args) {
		CommandLine.call(new App(), args);
	}

	@Parameters(index = "0", description = "The .knxproj file to convert.")
	private File knxProjFile;

	@Override
	public Void call() throws Exception {

		KnxProjectFile knxProjectFile = new KnxProjectFile(knxProjFile);
		knxProjectFile.open();

		return null;
	}
}
