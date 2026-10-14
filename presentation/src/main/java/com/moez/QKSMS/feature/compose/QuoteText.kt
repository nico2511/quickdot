/*
 * Copyright (C) 2026 nico2511
 *
 * This file is part of Quickdot.
 *
 * Quickdot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.octoshrimpy.quik.feature.compose

/**
 * Plain-text reply quote. SMS has no reply metadata, so the quote rides in the
 * body in a form any phone can show.
 *
 * Guillemets and the ellipsis character are not in the GSM-7 alphabet and would
 * force a 70-character Unicode segment. The marker is ASCII plus one extension
 * character (`>`), and a cut excerpt ends with three periods.
 *
 *     > Name: first 80 characters...
 *     the reply
 */
object QuoteText {

    const val EXCERPT_LIMIT = 80
    private const val NAME_LIMIT = 40

    private val whitespace = Regex("\\s+")
    private val pattern = Regex("^> ([^\\n:]{1,$NAME_LIMIT}): ([^\\n]{1,160})\\n([\\s\\S]*)$")

    data class Parsed(
        val name: String,
        val excerpt: String,
        val reply: String,
    )

    fun sanitizeName(name: String): String {
        val cleaned = name
            .replace(':', ' ')
            .replace(whitespace, " ")
            .trim()
            .take(NAME_LIMIT)
            .trim()
        return cleaned.ifBlank { "?" }
    }

    /** One line, at most [EXCERPT_LIMIT] characters, newlines collapsed. */
    fun excerpt(text: String, limit: Int = EXCERPT_LIMIT): String {
        val flat = text.replace(whitespace, " ").trim()
        if (flat.length <= limit) return flat
        return flat.take(limit - 3).trimEnd() + "..."
    }

    fun quoteLine(name: String, excerpt: String): String {
        return "> ${sanitizeName(name)}: ${excerpt.replace(whitespace, " ").trim()}"
    }

    /** Full SMS body. An empty reply still keeps the quote line and the newline. */
    fun composeBody(name: String, excerpt: String, reply: String): String {
        val line = quoteLine(name, excerpt)
        val rest = reply.trimStart('\n', '\r')
        return if (rest.isEmpty()) "$line\n" else "$line\n$rest"
    }

    /**
     * A quote is only a quote when the first line matches and a newline follows.
     * A single line the user typed, such as `> note: hi`, stays ordinary text.
     */
    fun parse(body: String): Parsed? {
        val match = pattern.matchEntire(body) ?: return null
        return Parsed(
            name = match.groupValues[1],
            excerpt = match.groupValues[2],
            reply = match.groupValues[3],
        )
    }
}
