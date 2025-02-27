package io.gitlab.arturbosch.detekt.api.internal

import io.github.detekt.psi.absolutePath
import io.github.detekt.test.utils.resourceAsPath
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class InclusionExclusionPatternsSpec {

    @Nested
    inner class `rule should only run on library file specified by 'includes' pattern` {

        private val config = TestConfig(Config.INCLUDES_KEY to "**/library/*.kt")

        @Test
        fun `should run`() {
            resourceAsPath("library/Library.kt")
                .runWith(DummyRule(config))
                .assertWasVisited()
        }

        @Test
        fun `should not run`() {
            resourceAsPath("Default.kt")
                .runWith(DummyRule(config))
                .assertNotVisited()
        }
    }

    @Nested
    inner class `rule should only run on library file not matching the specified 'excludes' pattern` {

        private val config = TestConfig(Config.EXCLUDES_KEY to "glob:**/Default.kt")

        @Test
        fun `should run`() {
            resourceAsPath("library/Library.kt")
                .runWith(DummyRule(config))
                .assertWasVisited()
        }

        @Test
        fun `should not run`() {
            resourceAsPath("Default.kt")
                .runWith(DummyRule(config))
                .assertNotVisited()
        }
    }

    @Nested
    inner class `rule should report on both runs without config` {

        @Test
        fun `should run on library file`() {
            resourceAsPath("library/Library.kt")
                .runWith(DummyRule())
                .assertWasVisited()
        }

        @Test
        fun `should run on non library file`() {
            resourceAsPath("Default.kt")
                .runWith(DummyRule())
                .assertWasVisited()
        }
    }

    @Nested
    inner class `rule should only run on included files` {

        @Test
        fun `should only run on dummies`() {
            val config = TestConfig(
                Config.INCLUDES_KEY to "**/library/**",
                Config.EXCLUDES_KEY to "**Library.kt",
            )

            OnlyLibraryTrackingRule(config).apply {
                Files.walk(resourceAsPath("library/Library.kt").parent)
                    .filter { it.isRegularFile() }
                    .forEach { this.lint(it) }
                assertOnlyLibraryFileVisited(false)
                assertCounterWasCalledTimes(2)
            }
        }

        @Test
        fun `should only run on library file`() {
            val config = TestConfig(
                Config.INCLUDES_KEY to "**/library/**",
                Config.EXCLUDES_KEY to "**Dummy*.kt",
            )

            OnlyLibraryTrackingRule(config).apply {
                Files.walk(resourceAsPath("library/Library.kt").parent)
                    .filter { it.isRegularFile() }
                    .forEach { this.lint(it) }
                assertOnlyLibraryFileVisited(true)
                assertCounterWasCalledTimes(0)
            }
        }
    }
}

private fun Path.runWith(rule: DummyRule): DummyRule {
    rule.lint(this)
    return rule
}

private class OnlyLibraryTrackingRule(config: Config) : Rule(config) {

    override val issue: Issue = Issue("test", "", Debt.FIVE_MINS)
    private var libraryFileVisited = false
    private var counter = 0

    override fun visitKtFile(file: KtFile) {
        if ("Library.kt" in file.absolutePath().toString()) {
            libraryFileVisited = true
        } else {
            counter++
        }
    }

    fun assertOnlyLibraryFileVisited(wasVisited: Boolean) {
        assertThat(libraryFileVisited).isEqualTo(wasVisited)
    }

    fun assertCounterWasCalledTimes(size: Int) {
        assertThat(counter).isEqualTo(size)
    }
}

private class DummyRule(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue("test", "", Debt.FIVE_MINS)
    private var isDirty: Boolean = false

    override fun visitKtFile(file: KtFile) {
        isDirty = true
    }

    fun assertWasVisited() {
        assertThat(isDirty).isTrue()
    }

    fun assertNotVisited() {
        assertThat(isDirty).isFalse()
    }
}
