package com.here.provenanceanalyzer.functionaltest

import com.here.provenanceanalyzer.managers.NPM

import io.kotlintest.TestCaseContext
import io.kotlintest.matchers.endWith
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.matchers.startWith
import io.kotlintest.specs.StringSpec

import java.io.File

class NpmTest : StringSpec() {
    private val projectBaseDir = File("src/funTest/assets/projects/synthetic/project-npm")

    @Suppress("CatchException")
    override fun interceptTestCase(context: TestCaseContext, test: () -> Unit) {
        try {
            test()
        } catch (exception: Exception) {
            // Make sure the node_modules directory is always deleted from each subdirectory to prevent side-effects
            // from failing tests.
            projectBaseDir.listFiles().forEach {
                if (it.isDirectory) {
                    val nodeModulesDir = File(it, "node_modules")
                    if (nodeModulesDir.isDirectory) {
                        nodeModulesDir.deleteRecursively()
                    }
                }
            }

            throw exception
        }
    }

    init {
        "yarn dependencies are resolved correctly" {
            val workingDir = File(projectBaseDir, "yarn")
            val packageFile = File(workingDir, "package.json")
            val expectedDependencies = File(
                    "src/funTest/assets/projects/synthetic/project-npm-expected-yarn-dependencies.txt").readText()

            val resolvedDependencies = NPM.resolveDependencies(listOf(packageFile))[packageFile].toString()

            NPM.command(workingDir) shouldBe NPM.yarn
            resolvedDependencies shouldBe expectedDependencies
        }

        "NPM shrinkwrap dependencies are resolved correctly" {
            val workingDir = File(projectBaseDir, "shrinkwrap")
            val packageFile = File(workingDir, "package.json")
            val expectedDependencies = File(
                    "src/funTest/assets/projects/synthetic/project-npm-expected-npm-dependencies.txt").readText()

            val resolvedDependencies = NPM.resolveDependencies(listOf(packageFile))[packageFile].toString()

            NPM.command(workingDir) shouldBe NPM.npm
            resolvedDependencies shouldBe expectedDependencies
        }

        "NPM package-lock dependencies are resolved correctly" {
            val workingDir = File(projectBaseDir, "package-lock")
            val packageFile = File(workingDir, "package.json")
            val expectedDependencies = File(
                    "src/funTest/assets/projects/synthetic/project-npm-expected-npm-dependencies.txt").readText()

            val resolvedDependencies = NPM.resolveDependencies(listOf(packageFile))[packageFile].toString()

            NPM.command(workingDir) shouldBe NPM.npm
            resolvedDependencies shouldBe expectedDependencies
        }

        "NPM aborts when no lockfile is present" {
            val workingDir = File(projectBaseDir, "no-lockfile")
            val packageFile = File(workingDir, "package.json")

            val exception = shouldThrow<IllegalArgumentException> {
                NPM.resolveDependencies(listOf(packageFile))
            }
            @Suppress("UnsafeCallOnNullableType")
            exception.message!! should startWith("No lockfile found in")
        }

        "NPM aborts when multiple lockfiles are present" {
            val workingDir = File(projectBaseDir, "multiple-lockfiles")
            val packageFile = File(workingDir, "package.json")

            val exception = shouldThrow<IllegalArgumentException> {
                NPM.resolveDependencies(listOf(packageFile))
            }
            @Suppress("UnsafeCallOnNullableType")
            exception.message!! should endWith("contains multiple lockfiles. It is ambiguous which one to use.")
        }

        "NPM aborts when the node_modules directory already exists" {
            val workingDir = File(projectBaseDir, "node-modules")
            val packageFile = File(workingDir, "package.json")

            val exception = shouldThrow<IllegalArgumentException> {
                NPM.resolveDependencies(listOf(packageFile))
            }
            @Suppress("UnsafeCallOnNullableType")
            exception.message!! should endWith("directory already exists.")
        }
    }
}
