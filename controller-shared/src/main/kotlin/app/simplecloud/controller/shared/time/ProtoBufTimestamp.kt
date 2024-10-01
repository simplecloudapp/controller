package app.simplecloud.controller.shared.time

import com.google.protobuf.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object ProtoBufTimestamp {
    fun toLocalDateTime(timestamp: Timestamp): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong()), ZoneId.systemDefault())
    }

    fun fromLocalDateTime(localDateTime: LocalDateTime): Timestamp {
        val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()

        return Timestamp.newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()
    }
}