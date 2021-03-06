package com.github.easypack.mojo;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.easypack.constants.FolderConstants;
import com.github.easypack.io.IoFactory;
import com.github.easypack.io.PathUtils;
import com.github.easypack.platform.Platform;
import com.github.easypack.platform.PlatformBehavioural;
import com.github.easypack.script.PreStart;
import com.github.easypack.script.ShutdownScriptWriter;
import com.github.easypack.script.StartScriptWriter;

/**
 * Creates the application scripts to be included in the final artifact.
 * 
 * @author agusmunioz
 * 
 */
@Mojo(name = "scripts")
public class CreateScriptsMojo extends AbstractMojo {

	@Parameter(property = "project.build.directory", readonly = true)
	private String outputDirectory;

	@Parameter(property = "project.build.finalName", readonly = true)
	private String finalName;

	/**
	 * Java Virtual Machine options, like maximum heap size (-Xmx3G) or
	 * environment properties specified with -D.
	 */
	@Parameter(defaultValue = "")
	private String opts = "";

	/**
	 * Arguments passed to the application main method.
	 * 
	 */
	@Parameter(defaultValue = "")
	private String args = "";

	/**
	 * A comma separated list of platforms, the start script must be created
	 * for. Supported values: windows, linux.
	 */
	@Parameter(defaultValue = "linux, windows")
	private String platforms;

	/**
	 * Script/s to be executed before application startup. Will be merged in the
	 * final start script/s. Default names bin/start-linux, bin/start-windows.
	 */
	@Parameter(alias = "start")
	private PreStart preStart = new PreStart();

	/**
	 * Defines which part of the start script must be echoed when executed.
	 * Possible values are: all (for echoing the entire script) or java (for
	 * echoing only the java line). By default no echoing is performed.
	 */
	@Parameter(defaultValue = "")
	private String echo;

	/**
	 * Indicates if a shutdown script must be generated. Only supported for
	 * Linux platform. Default: false.
	 */
	@Parameter(defaultValue = "false")
	private Boolean shutdown = false;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		File folder = IoFactory.file(PathUtils.path(this.outputDirectory,
				FolderConstants.BIN));

		if (folder.exists()) {
			folder.delete();
		}

		folder.mkdir();

		try {

			for (Platform platform : Platform.fromString(this.platforms)) {

				for (PlatformBehavioural<Void> writer : this.getWriters(folder)) {
					platform.behave(writer);
				}

			}

		} catch (Exception e) {
			throw new MojoExecutionException(
					"Exception while creating scripts.", e);
		}

	}

	/**
	 * Gets the scripts writers for creating the script files.
	 * 
	 * @param folder
	 *            the scripts destination folder.
	 * 
	 * @return the list of script writers.
	 */
	private Collection<PlatformBehavioural<Void>> getWriters(File folder) {

		Collection<PlatformBehavioural<Void>> writers = new LinkedList<PlatformBehavioural<Void>>();

		StartScriptWriter startup = new StartScriptWriter().args(this.args)
				.opts(this.opts).echo(this.echo).jar(this.finalName)
				.folder(folder).process(this.finalName).preStart(this.preStart);

		writers.add(startup);

		if (this.shutdown) {

			writers.add(new ShutdownScriptWriter().process(this.finalName)
					.folder(folder));
		}

		return writers;
	}

}
