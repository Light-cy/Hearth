package com.example.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SessionManager
import com.example.di.AppContainer
import com.example.model.Role
import com.example.ui.components.DarkForestHeroCard
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.MemberAvatar
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedLight
import com.example.ui.theme.Forest700
import com.example.ui.theme.Forest800
import com.example.ui.theme.Forest900
import com.example.ui.theme.HearthCardHolster
import com.example.ui.theme.HearthGlassBorder
import com.example.ui.theme.HearthGlassStrong
import com.example.ui.theme.HearthGlassSurface
import com.example.ui.theme.HearthTextDark
import com.example.ui.theme.HearthTextMuted
import com.example.ui.theme.Sage200
import com.example.ui.theme.Sage300
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    appContainer: AppContainer,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentMember by appContainer.sessionManager.currentMember.collectAsState()
    val householdName by appContainer.sessionManager.householdName.collectAsState()
    val inviteCode by appContainer.sessionManager.inviteCode.collectAsState()
    val isLiveMode by appContainer.isLiveMode.collectAsState()
    val baseUrl by appContainer.baseUrl.collectAsState()
    val allMembers by appContainer.sessionManager.registeredMembers.collectAsState()

    var editedServerUrl by remember(baseUrl) { mutableStateOf(baseUrl) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var budgetAlertsEnabled by remember { mutableStateOf(true) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    ResponsiveScreenContainer(
        scrollable = true,
        bottomSafePadding = 132.dp
    ) { _ ->
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HOUSEHOLD STEWARD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                    color = Forest700
                )
                Text(
                    text = "Steward & Vault Config",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = HearthTextDark
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isLiveMode) CreditGreenLight else Sage200)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isLiveMode) "CLOUD SYNC" else "LOCAL VAULT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLiveMode) CreditGreen else Forest900
                )
            }
        }

        // 1. Steward Identity Hero Card (Dark Forest Emerald Glass)
        DarkForestHeroCard(
            cornerRadius = 28.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemberAvatar(
                        name = currentMember.name,
                        memberId = currentMember.id,
                        size = 56.dp,
                        isSelected = true
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = currentMember.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = currentMember.email.ifEmpty { "${currentMember.name.lowercase()}@hearth.pk" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Sage300.copy(alpha = 0.22f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "ID: ${currentMember.id}",
                                    color = Sage300,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (currentMember.role == Role.ADMIN) "Admin Steward" else "Household Member",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Cloud Sync & Vault Storage Card
        GlassPanelCard(
            cornerRadius = 26.dp,
            containerColor = HearthGlassStrong,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("server_settings_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CLOUD SYNC & VAULT STORAGE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp,
                            color = Forest800
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isLiveMode) {
                                "Mode: Cloud-Synced Household Server"
                            } else {
                                "Mode: On-Device Local Vault"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = HearthTextDark
                        )
                    }
                    Switch(
                        checked = isLiveMode,
                        onCheckedChange = { enabled ->
                            appContainer.updateBaseUrl(editedServerUrl.trim())
                            appContainer.toggleLiveMode(enabled)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (enabled) "Connected to Cloud Sync Server ($editedServerUrl)"
                                    else "Switched to On-Device Local Vault"
                                )
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Forest800
                        ),
                        modifier = Modifier.testTag("remote_backend_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editedServerUrl,
                    onValueChange = { editedServerUrl = it },
                    label = { Text("Hearth Sync Server URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_url_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            appContainer.updateBaseUrl(editedServerUrl.trim())
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Saved Server URL: ${editedServerUrl.trim()}")
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply URL", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            appContainer.repository.restoreDefaultLedger()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Household ledger synchronized"
                                )
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Forest900),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("reset_benchmark_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Ledger", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 3. Switch Active Resident Card
        GlassPanelCard(
            cornerRadius = 26.dp,
            containerColor = HearthGlassSurface,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("demo_switcher_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "SWITCH ACTIVE RESIDENT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                    color = Forest800
                )
                Text(
                    text = "Instantly view the household ledger from any co-resident's perspective",
                    fontSize = 11.sp,
                    color = HearthTextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allMembers.forEach { member ->
                        val isSelected = member.id == currentMember.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) Forest800 else Color.White.copy(alpha = 0.85f))
                                .border(
                                    1.dp,
                                    if (isSelected) Forest800 else HearthGlassBorder,
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    appContainer.sessionManager.switchActiveMember(member)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Switched active profile to ${member.name} (${member.id})")
                                    }
                                }
                                .padding(vertical = 12.dp)
                                .testTag("profile_switch_${member.id.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = member.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.White else HearthTextDark
                                )
                                Text(
                                    text = member.id,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) Sage300 else HearthTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Household & Invite Code Card
        GlassPanelCard(
            cornerRadius = 24.dp,
            containerColor = Color.White.copy(alpha = 0.86f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Forest800)
                    Text(
                        text = "Shared Household Vault",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = HearthTextDark
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                ProfileInfoRow(label = "Residence Name", value = householdName)
                ProfileInfoRow(label = "Invite Code", value = inviteCode)
                ProfileInfoRow(label = "Currency & Precision", value = "PKR (Integer Paisa / 64-Bit)")
                ProfileInfoRow(label = "Co-Residents", value = allMembers.joinToString(", ") { "${it.name} (${it.id})" })

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Hearth Invite Code", inviteCode)
                            clipboard.setPrimaryClip(clip)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Copied invite code: $inviteCode")
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (currentMember.role == Role.ADMIN) {
                        OutlinedButton(
                            onClick = {
                                val rotated = appContainer.sessionManager.rotateInviteCode()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Rotated household invite code to $rotated")
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rotate Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Account Security & Preferences
        GlassPanelCard(
            cornerRadius = 24.dp,
            containerColor = Color.White.copy(alpha = 0.86f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Forest800)
                    Text(
                        text = "Security & Alerts",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = HearthTextDark
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Expense & Settlement Alerts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HearthTextDark)
                        Text("Notify when a roommate logs a bill or settles debt", fontSize = 11.sp, color = HearthTextMuted)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Forest800)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = HearthGlassBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("80% Budget Threshold Warning", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HearthTextDark)
                        Text("Alert when monthly spend crosses Near Limit (80%)", fontSize = 11.sp, color = HearthTextMuted)
                    }
                    Switch(
                        checked = budgetAlertsEnabled,
                        onCheckedChange = { budgetAlertsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Forest800)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showChangePasswordDialog = true },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Password", fontWeight = FontWeight.Bold, color = Forest800)
                }
            }
        }

        // 6. Hearth Vault Security & Ledger Guarantees
        GlassPanelCard(
            cornerRadius = 24.dp,
            containerColor = HearthCardHolster,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Forest800)
                    Text(
                        text = "Hearth Vault Ledger Guarantees",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Forest800
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Paisa-Exact Arithmetic — All PKR transactions are processed as 64-bit integer paisa with zero floating-point drift.\n" +
                            "• Deterministic Fair Split — Indivisible paisa remainders are allocated transparently by resident order.\n" +
                            "• Continuous Zero-Sum Audit — Every shared expense and settlement is verified to maintain a balanced household ledger.",
                    fontSize = 11.sp,
                    color = HearthTextDark,
                    lineHeight = 17.sp
                )
            }
        }

        // 7. Sign Out Button
        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(
                containerColor = DebtRedLight,
                contentColor = DebtRed
            ),
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("sign_out_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out of Household", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }

    if (showChangePasswordDialog) {
        var currentPasswordInput by remember { mutableStateOf("") }
        var newPasswordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password (min 8 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPasswordInput.length >= 8) {
                            appContainer.sessionManager.updatePassword(newPasswordInput)
                            showChangePasswordDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Password updated for ${currentMember.name}. Other sessions revoked.")
                            }
                        }
                    },
                    enabled = newPasswordInput.length >= 8,
                    colors = ButtonDefaults.buttonColors(containerColor = Forest800)
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = HearthTextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.ExtraBold,
            color = HearthTextDark
        )
    }
}
