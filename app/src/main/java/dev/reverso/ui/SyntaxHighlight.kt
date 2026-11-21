package dev.reverso.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ReversoSyntaxHighlight : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val builder = AnnotatedString.Builder(source)
        val rules = listOf(
            Regex("\\b(fun|fn|class|struct|enum|interface|object|data|sealed|val|var|const|let|mut|return|if|else|when|match|for|while|loop|import|package|use|mod|pub|async|await|go|func|def|type|impl|trait|self|true|false|null|nil)\\b") to ReversoColors.Purple,
            Regex("\\b[0-9]+(?:\\.[0-9]+)?\\b") to ReversoColors.Warning,
            Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'") to ReversoColors.Mint,
            Regex("//[^\\n]*|#[^\\n]*") to ReversoColors.Muted,
        )
        rules.forEach { (pattern, color) ->
            pattern.findAll(source).forEach { match ->
                builder.addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
