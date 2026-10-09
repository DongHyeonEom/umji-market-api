package com.buyeong.umji.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

private const val RULE_SET_ID = "umji"
private const val MAX_COMBINED_ANNOTATION_LENGTH = 50

class UmjiRuleSetProvider : RuleSetProviderV3(RuleSetId(RULE_SET_ID)) {
    override fun getRuleProviders(): Set<RuleProvider> = setOf(RuleProvider { SchemaFieldLayoutRule() })
}

class SchemaFieldLayoutRule : Rule(RuleId("$RULE_SET_ID:schema-field-layout"), About()) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType != KtNodeTypes.KT_FILE) return

        val source = node.text
        val violations = mutableListOf<Pair<Int, String>>()
        addMatches(source, Regex("@field:Schema\\([^\\r\\n]*\\)[ \\t]+(?:val|var)\\s+"), "Move the property declaration below @field:Schema", violations)
        addMatches(source, Regex("(?m)^\\s*\\)\\s+(?:val|var)\\s+"), "Move the property declaration below the annotation", violations)
        addMatches(source, Regex("(?m)^[ \\t]*(?:val|var)\\s+[^\\r\\n]*,[ \\t]*\\r?\\n(?=[ \\t]*@field:)"), "Add a blank line after the field", violations)
        addMatches(source, Regex("@field:Size\\([^)]*\\r?\\n[^)]*\\)"), "Keep numeric @field:Size arguments on one line", violations)
        source.lineSequence().forEachIndexed { index, line ->
            if (Regex("@field:").findAll(line).count() > 1 && line.length > MAX_COMBINED_ANNOTATION_LENGTH) {
                val offset = source.lineSequence().take(index).sumOf { it.length + 1 }
                violations += offset to "Split field annotations when their combined line exceeds $MAX_COMBINED_ANNOTATION_LENGTH characters"
            }
        }

        violations.distinct().forEach { (offset, message) -> emit(node.startOffset + offset, message, false) }
    }

    private fun addMatches(
        source: String,
        regex: Regex,
        message: String,
        violations: MutableList<Pair<Int, String>>,
    ) {
        regex.findAll(source).forEach { violations += it.range.first to message }
    }
}
