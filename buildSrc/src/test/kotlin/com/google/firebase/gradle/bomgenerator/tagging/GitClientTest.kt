package com.google.firebase.gradle.bomgenerator.tagging

import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GitClientTest {
  @Rule @JvmField val testGitDirectory = TemporaryFolder()
  private val branch = AtomicReference<String>()
  private val commit =  AtomicReference<String>()

  @Before
  fun setup() {
    testGitDirectory.newFile("hello.txt").writeText("hello git!")
    val executor = ShellExecutor(testGitDirectory.root)
    val handler : (List<String>) -> Unit =  { it.forEach(System.out::println) }
    executor.execute("git init", handler)
    executor.execute("git commit --allow-empty -m 'init'", handler)

    executor.execute("git rev-parse --abbrev-ref HEAD") { branch.set(it[0]) }
    executor.execute("git rev-parse HEAD") { commit.set(it[0]) }
  }

  @Test
  fun `tag M release version succeeds on local file system`() {
    val executor = ShellExecutor(testGitDirectory.root)
    val git = GitClient(branch.get(), commit.get(), executor, System.out::println)
    git.tagReleaseVersion()
    executor.execute("git tag --points-at HEAD") {
      println(it)
      Assert.assertTrue(it.stream().anyMatch { x -> x.contains(branch.get()) })
    }
  }

  @Test
  fun `tag bom version succeeds on local file system`() {
    val executor = ShellExecutor(testGitDirectory.root)
    val git = GitClient(branch.get(), commit.get(), executor, System.out::println);
    git.tagBomVersion("1.2.3")
    executor.execute("git tag --points-at HEAD") {
      Assert.assertTrue(it.stream().anyMatch { x -> x.contains("bom@1.2.3") })
    }
  }

  @Test
  fun `tag product version succeeds on local file system`() {
    val executor = ShellExecutor(testGitDirectory.root)
    val git = GitClient(branch.get(), commit.get(), executor, System.out::println)
    git.tagProductVersion("firebase-database", "1.2.3")
    executor.execute("git tag --points-at HEAD") {
      Assert.assertTrue(it.stream().anyMatch { x -> x.contains("firebase-database@1.2.3") })
    }
  }

  @Test
  fun `tags are pushed to both private and public repositories`() {
    Assume.assumeTrue(System.getenv().containsKey("FIREBASE_CI"));

    val mockExecutor = object : ShellExecutor(testGitDirectory.root) {
      override fun execute(command: String, consumer: Consumer<List<String>>) {
        consumer.accept(listOf("Executed: $command"))
      }
    }

    val outputs = mutableListOf<String>()
    val git =  GitClient(branch.get(), commit.get(), mockExecutor) { outputs.add(it) }
    git.tagBomVersion("1.2.3")
    git.tagProductVersion("firebase-functions", "1.2.3")
    git.pushCreatedTags()
    
    Assert.assertTrue(outputs.stream().anyMatch { it.contains("git push origin") })
    Assert.assertTrue(outputs.stream().anyMatch { it.contains("git push public") })
  }
}
