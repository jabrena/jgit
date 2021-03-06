/*
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.junit.Test;

/**
 * Tests for setting up the working directory when creating a Repository
 */
public class RepositorySetupWorkDirTest extends LocalDiskRepositoryTestCase {

	@Test
	public void testIsBare_CreateRepositoryFromArbitraryGitDir()
			throws Exception {
		File gitDir = getFile("workdir");
		Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build();
		assertTrue(repo.isBare());
	}

	@Test
	public void testNotBare_CreateRepositoryFromDotGitGitDir() throws Exception {
		File gitDir = getFile("workdir", Constants.DOT_GIT);
		Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build();
		assertFalse(repo.isBare());
		assertWorkdirPath(repo, "workdir");
		assertGitdirPath(repo, "workdir", Constants.DOT_GIT);
	}

	@Test
	public void testWorkdirIsParentDir_CreateRepositoryFromDotGitGitDir()
			throws Exception {
		File gitDir = getFile("workdir", Constants.DOT_GIT);
		Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build();
		String workdir = repo.getWorkTree().getName();
		assertEquals(workdir, "workdir");
	}

	@Test
	public void testNotBare_CreateRepositoryFromWorkDirOnly() throws Exception {
		File workdir = getFile("workdir", "repo");
		Repository repo = new FileRepositoryBuilder().setWorkTree(workdir)
				.build();
		assertFalse(repo.isBare());
		assertWorkdirPath(repo, "workdir", "repo");
		assertGitdirPath(repo, "workdir", "repo", Constants.DOT_GIT);
	}

	@Test
	public void testWorkdirIsDotGit_CreateRepositoryFromWorkDirOnly()
			throws Exception {
		File workdir = getFile("workdir", "repo");
		Repository repo = new FileRepositoryBuilder().setWorkTree(workdir)
				.build();
		assertGitdirPath(repo, "workdir", "repo", Constants.DOT_GIT);
	}

	@Test
	public void testNotBare_CreateRepositoryFromGitDirOnlyWithWorktreeConfig()
			throws Exception {
		File gitDir = getFile("workdir", "repoWithConfig");
		File workTree = getFile("workdir", "treeRoot");
		setWorkTree(gitDir, workTree);
		Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build();
		assertFalse(repo.isBare());
		assertWorkdirPath(repo, "workdir", "treeRoot");
		assertGitdirPath(repo, "workdir", "repoWithConfig");
	}

	@Test
	public void testBare_CreateRepositoryFromGitDirOnlyWithBareConfigTrue()
			throws Exception {
		File gitDir = getFile("workdir", "repoWithConfig");
		setBare(gitDir, true);
		Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build();
		assertTrue(repo.isBare());
	}

	@Test
	public void testWorkdirIsParent_CreateRepositoryFromGitDirOnlyWithBareConfigFalse()
			throws Exception {
		File gitDir = getFile("workdir", "repoWithBareConfigTrue", "child");
		setBare(gitDir, false);
		Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build();
		assertWorkdirPath(repo, "workdir", "repoWithBareConfigTrue");
	}

	@Test
	public void testNotBare_CreateRepositoryFromGitDirOnlyWithBareConfigFalse()
			throws Exception {
		File gitDir = getFile("workdir", "repoWithBareConfigFalse", "child");
		setBare(gitDir, false);
		Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build();
		assertFalse(repo.isBare());
		assertWorkdirPath(repo, "workdir", "repoWithBareConfigFalse");
		assertGitdirPath(repo, "workdir", "repoWithBareConfigFalse", "child");
	}

	@Test
	public void testExceptionThrown_BareRepoGetWorkDir() throws Exception {
		File gitDir = getFile("workdir");
		try (Repository repo = new FileRepository(gitDir)) {
			repo.getWorkTree();
			fail("Expected NoWorkTreeException missing");
		} catch (NoWorkTreeException e) {
			// expected
		}
	}

	@Test
	public void testExceptionThrown_BareRepoGetIndex() throws Exception {
		File gitDir = getFile("workdir");
		try (Repository repo = new FileRepository(gitDir)) {
			repo.readDirCache();
			fail("Expected NoWorkTreeException missing");
		} catch (NoWorkTreeException e) {
			// expected
		}
	}

	@Test
	public void testExceptionThrown_BareRepoGetIndexFile() throws Exception {
		File gitDir = getFile("workdir");
		try (Repository repo = new FileRepository(gitDir)) {
			repo.getIndexFile();
			fail("Expected NoWorkTreeException missing");
		} catch (NoWorkTreeException e) {
			// expected
		}
	}

	private File getFile(String... pathComponents) throws IOException {
		File dir = getTemporaryDirectory();
		for (String pathComponent : pathComponents)
			dir = new File(dir, pathComponent);
		FileUtils.mkdirs(dir, true);
		return dir;
	}

	private void setBare(File gitDir, boolean bare) throws IOException,
			ConfigInvalidException {
		FileBasedConfig cfg = configFor(gitDir);
		cfg.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_BARE, bare);
		cfg.save();
	}

	private void setWorkTree(File gitDir, File workTree)
			throws IOException,
			ConfigInvalidException {
		String path = workTree.getAbsolutePath();
		FileBasedConfig cfg = configFor(gitDir);
		cfg.setString(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_WORKTREE, path);
		cfg.save();
	}

	private FileBasedConfig configFor(File gitDir) throws IOException,
			ConfigInvalidException {
		File configPath = new File(gitDir, Constants.CONFIG);
		FileBasedConfig cfg = new FileBasedConfig(configPath, FS.DETECTED);
		cfg.load();
		return cfg;
	}

	private void assertGitdirPath(Repository repo, String... expected)
			throws IOException {
		File exp = getFile(expected).getCanonicalFile();
		File act = repo.getDirectory().getCanonicalFile();
		assertEquals("Wrong Git Directory", exp, act);
	}

	private void assertWorkdirPath(Repository repo, String... expected)
			throws IOException {
		File exp = getFile(expected).getCanonicalFile();
		File act = repo.getWorkTree().getCanonicalFile();
		assertEquals("Wrong working Directory", exp, act);
	}
}
