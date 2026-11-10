package com.onlyfield.assetmanager.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatDateTime(epochMs: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date(epochMs))

internal fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().replace('.', ',')

internal fun safeFileName(name: String): String = name.trim().replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").ifBlank { "progetto" }
