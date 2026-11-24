package com.lsing.timego.domain

import java.time.LocalDate

data class RecordEntry(val record: PersonalRecord, val isNew: Boolean)

/** Every record, newest first; "new" means set on or after [since]. */
fun recordsOverview(records: List<PersonalRecord>, since: LocalDate): List<RecordEntry> =
    records.sortedByDescending { it.achievedOn }.map { RecordEntry(it, !it.achievedOn.isBefore(since)) }
