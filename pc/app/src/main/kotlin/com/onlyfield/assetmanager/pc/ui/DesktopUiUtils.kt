package com.onlyfield.assetmanager.pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.exchange.ComparisonStatus

@Composable
fun EmptyStateCard(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (actionLabel != null && onAction != null) {
                    Button(onClick = onAction) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

fun MountingType.toDisplayString(): String = when (this) {
    MountingType.RACK_MOUNT -> "Montaggio a Rack"
    MountingType.VERTICAL_MOUNT -> "Montaggio Verticale"
    MountingType.SHELF_MOUNT -> "Ripiano / Mensola"
    MountingType.OUT_OF_RACK -> "Fuori Rack / Esterno"
}

fun RackSide.toDisplayString(): String = when (this) {
    RackSide.FRONT -> "Fronte (Front)"
    RackSide.REAR -> "Retro (Rear)"
    RackSide.BOTH -> "Fronte e Retro (Entrambi)"
}

fun DeviceCategory.toDisplayString(): String = when (this) {
    DeviceCategory.NETWORK_SWITCH -> "Switch di Rete"
    DeviceCategory.PATCH_PANEL -> "Pannello di Permutazione"
    DeviceCategory.UPS_PDU -> "Alimentazione PDU / UPS"
    DeviceCategory.SERVER_STORAGE -> "Server / Storage"
    DeviceCategory.CAMERA_NVR -> "Videosorveglianza CCTV / NVR"
    DeviceCategory.SHELF -> "Ripiano Apparecchiature"
    DeviceCategory.BLANK_PANEL -> "Pannello Cieco"
    DeviceCategory.CUSTOM -> "Personalizzato / Altro"
}

fun ComparisonStatus.toDisplayString(): String = when (this) {
    ComparisonStatus.IDENTICAL -> "Identico alla copia attuale"
    ComparisonStatus.NEWER_REVISION -> "Aggiornamento disponibile (Copia in ingresso più recente)"
    ComparisonStatus.OLDER_REVISION -> "Avviso: Copia in ingresso meno recente"
    ComparisonStatus.DIVERGENT -> "Revisioni divergenti"
    ComparisonStatus.DIFFERENT_PROJECT -> "Progetto differente"
}
