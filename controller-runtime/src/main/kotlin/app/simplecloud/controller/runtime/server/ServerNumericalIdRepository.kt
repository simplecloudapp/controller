package app.simplecloud.controller.runtime.server

import java.util.concurrent.ConcurrentHashMap

class ServerNumericalIdRepository {

  private val numericalIds = ConcurrentHashMap<String, Set<Int>>()

  @Synchronized
  fun findNextNumericalId(group: String): Int {
    val numericalIds = findNumericalIds(group)
    var nextId = 1
    while (numericalIds.contains(nextId)) {
      nextId++
    }
    saveNumericalId(group, nextId)
    return nextId
  }

  fun saveNumericalId(group: String, id: Int) {
    numericalIds.compute(group) { _, v -> v?.plus(id) ?: setOf(id) }
  }

  @Synchronized
  fun removeNumericalId(group: String, id: Int): Boolean {
    return numericalIds.computeIfPresent(group) { _, v -> v.minus(id) } != null
  }

  private fun findNumericalIds(group: String): List<Int> {
    return numericalIds[group]?.toList() ?: emptyList()
  }

}
