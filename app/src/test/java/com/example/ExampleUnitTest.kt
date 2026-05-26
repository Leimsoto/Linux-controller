package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    private fun parseCommands(text: String): List<String> {
        val result = mutableListOf<String>()
        val triplePattern = "```(?:bash|sh|shell)?\\n?([\\s\\S]*?)```".toRegex()
        val tripleMatches = triplePattern.findAll(text)
        for (m in tripleMatches) {
            val cmd = m.groupValues[1].trim()
            if (cmd.isNotBlank()) {
                result.add(cmd)
            }
        }
        val singlePattern = "`([^`\\n]+)`".toRegex()
        val singleMatches = singlePattern.findAll(text)
        for (m in singleMatches) {
            val cmd = m.groupValues[1].trim()
            if (cmd.isNotBlank() && !result.contains(cmd) && cmd.length > 2) {
                result.add(cmd)
            }
        }
        return result
    }

    @Test
    fun testParseCommandsBalanced() {
        val text = "Aquí tienes algunos comandos:\n```bash\nls -la\n```\nTambién puedes usar `uname -a` para info."
        val cmds = parseCommands(text)
        assertEquals(2, cmds.size)
        assertEquals("ls -la", cmds[0])
        assertEquals("uname -a", cmds[1])
    }

    @Test
    fun testParseCommandsUnclosedTriple() {
        val text = "Un bloque sin cerrar en la respuesta de la IA:\n```bash\nfree -h\n" + "a".repeat(5000)
        val cmds = parseCommands(text)
        assertEquals(0, cmds.size) // No match if unclosed
    }

    @Test
    fun testParseCommandsUnclosedSingle() {
        val text = "Un comando corto sin cerrar `apt-get update" + "b".repeat(5000)
        val cmds = parseCommands(text)
        assertEquals(0, cmds.size)
    }
}
