package io.gitlab.arturbosch.detekt.api

import org.jetbrains.kotlin.psi.KtFile

/**
 * A context describes the storing and reporting mechanism of [Finding]'s inside a [Rule].
 * Additionally it handles suppression and aliases management.
 *
 * The detekt engine retrieves the findings after each KtFile visit and resets the context
 * before the next KtFile.
 */
interface Context {

    val findings: List<Finding>

    /**
     * Reports a single new violation.
     * By contract the implementation can check if
     * this finding is already suppressed and should not get reported.
     * An alias set can be given to additionally check if an alias was used when suppressing.
     * Additionally suppression by rule set id is supported.
     */
    fun report(finding: Finding, aliases: Set<String> = emptySet(), ruleSetId: RuleSetId? = null) {
        // no-op
    }

    /**
     * Same as [report] but reports a list of [findings].
     */
    fun report(findings: List<Finding>, aliases: Set<String> = emptySet(), ruleSetId: RuleSetId? = null) {
        findings.forEach { report(it, aliases, ruleSetId) }
    }

    /**
     * Clears previous findings.
     * Normally this is done on every new [KtFile] analyzed and should be called by clients.
     */
    fun clearFindings()
}
