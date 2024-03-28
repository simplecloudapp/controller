package app.simplecloud.controller.runtime.server

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimaps

class ServerNumericalIdRepository {


  private val numericalIds = Multimaps.synchronizedListMultimap(ArrayListMultimap.create<String, Int>())

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
    numericalIds.put(group, id)
  }

  fun removeNumericalId(group: String, id: Int) {
    numericalIds.remove(group, id)
  }

  private fun findNumericalIds(group: String): List<Int> {
    return numericalIds.get(group)
  }

}
