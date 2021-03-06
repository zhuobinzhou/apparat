/*
 * This file is part of Apparat.
 *
 * Copyright (C) 2010 Joa Ebert
 * http://www.joa-ebert.com/
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package apparat.embedding.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * @author Joa Ebert
 */
abstract class AbstractApparatMojo extends AbstractMojo {
	/**
	 * The Maven project.
	 *
	 * @parameter default-value="null" expression="${project}"
	 */
	protected MavenProject project;

	/**
	 * Additional override for the target file if it is not the default artifact of your project.
	 * @parameter expression="${apparat.overrideArtifact}"
	 */
	protected File overrideArtifact;

	/**
	 * Whether or not to fail if the <code>overrideArtifact</code> is missing.
	 *
	 * @parameter default-value="true" expression="${apparat.failIfOverrideIsMissing}"
	 */
	protected boolean failIfOverrideIsMissing;

	/**
	 * {@inheritDoc}
	 */
	@Override public void execute() throws MojoExecutionException, MojoFailureException {
		final MavenLogAdapter logAdapter = new MavenLogAdapter(getLog());

		try {
			apparat.log.Log.setLevel(logAdapter.getLevel());
			apparat.log.Log.addOutput(logAdapter);

			if(null == overrideArtifact) {
				if(null != project) {
					processArtifact(project.getArtifact());

					for(final Artifact artifact : project.getAttachedArtifacts()) {
						processArtifact(artifact);
					}
				}
			} else {
				if(!overrideArtifact.exists()) {
					if(failIfOverrideIsMissing) {
						throw new MojoFailureException("File "+overrideArtifact+" does not exist.");
					} else {
						getLog().warn("Override "+overrideArtifact+" is missing.");
						return;
					}
				}

				try {
					processFile(overrideArtifact);
				} catch(final Throwable cause) {
					throw new MojoExecutionException("Apparat execution failed.", cause);
				}
			}
		} finally {
			apparat.log.Log.removeOutput(logAdapter);
		}
	}

	private void processArtifact(final Artifact artifact) throws MojoExecutionException, MojoFailureException {
		if(null == artifact) {
			return;
		}

		final String artifactType = artifact.getType();
		if(artifactType.equals("swc") || artifactType.equals("swf")) {
			try {
				if(null != artifact.getFile()) {
					processFile(artifact.getFile());
				}
			} catch(final Throwable cause) {
				throw new MojoExecutionException("Apparat execution failed.", cause);
			}
		} else {
			getLog().debug("Skipped artifact since its type is "+artifactType+".");
		}
	}

	abstract protected void processFile(final File file) throws MojoExecutionException, MojoFailureException;
}
