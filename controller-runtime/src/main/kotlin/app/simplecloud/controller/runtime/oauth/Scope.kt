package app.simplecloud.controller.runtime.oauth

object Scope {

    private fun toRegex(scope: String): String {
        return "^${scope.trim().replace(".", "\\.").replace("*", ".+")}$"
    }

    fun fromString(scope: String, delimiter: String = " "): List<String> {
        val added = mutableListOf<String>()
        scope.split(delimiter).distinct().forEach { toParse ->
            if (added.any { Regex(toRegex(it)).matches(toParse) }) return@forEach
            val regex = Regex(toRegex(toParse))
            added.toList().forEach { present ->
                if(regex.matches(present)) {
                    added.remove(present)
                }
            }
            added.add(toParse)
        }
        return added
    }

    fun validate(requiredScope: List<String>, providedScope: List<String>): Boolean {
        providedScope.forEach { provided ->
            val regex = Regex(toRegex(provided))
            if (!requiredScope.any { required -> regex.matches(required) }) {
                return false
            }
        }
        return true
    }
}