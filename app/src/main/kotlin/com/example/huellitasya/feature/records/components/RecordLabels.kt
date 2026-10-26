package com.example.huellitasya.feature.records.components

import com.example.huellitasya.R
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordType
import java.time.format.DateTimeFormatter

/** Figma: "Fecha:10/09/2026". */
internal val RecordDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal val RecordCategory.labelRes: Int
    get() = when (this) {
        RecordCategory.VACCINES -> R.string.records_category_vaccines
        RecordCategory.HISTORY -> R.string.records_category_history
        RecordCategory.FILES -> R.string.records_category_files
    }

internal val RecordType.labelRes: Int
    get() = when (this) {
        RecordType.VACCINATION -> R.string.records_type_vaccination
        RecordType.CONSULTATION -> R.string.records_type_consultation
        RecordType.EXAM -> R.string.records_type_exam
        RecordType.OTHER -> R.string.records_type_other
    }
