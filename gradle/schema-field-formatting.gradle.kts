tasks.register("formatSchemaProperties") {
    group = "formatting"
    description = "Formats Kotlin @field:Schema annotations and constructor properties."
    doLast {
        val schemaProperty = Regex("(?m)^(.*@field:Schema\\(.*\\))[ \\t]+((?:val|var)\\s+.*)$")
        val closingSchemaProperty = Regex("(?m)^(\\s*\\))([ \\t]+)((?:val|var)\\s+.*)$")
        val inlineDataClassProperty = Regex("(?m)^(\\s*data class [A-Za-z0-9_]+)\\((.*@field:Schema\\(.*\\))\\s+((?:val|var)\\s+.*)\\)$")
        val sizeAnnotation = Regex("@field:Size\\(\\s*((?:(?:min|max)\\s*=\\s*\\d+\\s*,?\\s*)+)\\)", setOf(RegexOption.DOT_MATCHES_ALL))
        val combinedAnnotations = Regex("(?m)^([ \\t]*)(@field:[^\\r\\n]*)$")

        fileTree("src") { include("**/*.kt") }.forEach { sourceFile ->
            val original = sourceFile.readText()
            val lineEnding = if ("\r\n" in original) "\r\n" else "\n"
            var formatted = sizeAnnotation.replace(original) { match ->
                val arguments = match.groupValues[1]
                    .replace(Regex("\\s*,\\s*"), ", ")
                    .replace(Regex("\\s*=\\s*"), " = ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .trimEnd(',')
                "@field:Size($arguments)"
            }
            formatted = inlineDataClassProperty.replace(formatted) { match ->
                val classIndent = match.groupValues[1].takeWhile { it == ' ' || it == '\t' }
                val parameterIndent = "$classIndent    "
                "${match.groupValues[1]}($lineEnding$parameterIndent${match.groupValues[2]}$lineEnding$parameterIndent${match.groupValues[3]},$lineEnding$classIndent)"
            }
            formatted = schemaProperty.replace(formatted) { match ->
                "${match.groupValues[1]}$lineEnding${match.groupValues[1].takeWhile { it == ' ' || it == '\t' }}${match.groupValues[2]}"
            }
            formatted = closingSchemaProperty.replace(formatted) { match ->
                val indentation = match.groupValues[1].takeWhile { it == ' ' || it == '\t' }
                "${match.groupValues[1]}$lineEnding$indentation${match.groupValues[3]}"
            }
            formatted = combinedAnnotations.replace(formatted) { match ->
                val indentation = match.groupValues[1]
                val annotations = match.groupValues[2]
                if (annotations.count { it == '@' } < 2) {
                    match.value
                } else {
                    val parts = annotations.split(Regex("[ \\t]+(?=@field:)"))
                        .map(String::trim)
                    val packed = mutableListOf<String>()
                    var current = ""
                    parts.forEach { annotation ->
                        val candidate = if (current.isEmpty()) annotation else "$current $annotation"
                        if (indentation.length + candidate.length <= 50) {
                            current = candidate
                        } else {
                            if (current.isNotEmpty()) packed += current
                            current = annotation
                        }
                    }
                    if (current.isNotEmpty()) packed += current
                    packed.joinToString(lineEnding) { "$indentation$it" }
                }
            }
            val propertyBeforeAnnotation = Regex("(?m)^([ \\t]*(?:val|var)\\s+[^\\r\\n]*,)\\r?\\n(?=[ \\t]*@field:)")
            formatted = propertyBeforeAnnotation.replace(formatted) { match ->
                match.groupValues[1] + lineEnding + lineEnding
            }
            if (formatted != original) sourceFile.writeText(formatted)
        }
    }
}
