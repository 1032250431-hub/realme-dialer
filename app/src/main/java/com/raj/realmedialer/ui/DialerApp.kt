package com.raj.realmedialer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Ink = Color(0xFF17181C)
private val Muted = Color(0xFF74767D)
private val Glass = Color(0xBFFFFFFF)
private val BgTop = Color(0xFFF9F9F7)
private val BgBottom = Color(0xFFE7EAE7)
private data class ContactItem(val name: String, val number: String)
private data class RecentItem(val name: String, val number: String, val type: Int, val time: String)

@Composable
fun DialerApp(onRequestDefaultDialer: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var number by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(emptyList<ContactItem>()) }
    var recents by remember { mutableStateOf(emptyList<RecentItem>()) }
    var showDefaultPrompt by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.READ_CONTACTS] == true || result[Manifest.permission.READ_CALL_LOG] == true) {
            contacts = loadContacts(context); recents = loadRecents(context)
        }
    }
    fun ensurePermissions() {
        val needed = buildList {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_CONTACTS)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.READ_CALL_LOG)
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray()) else { contacts = loadContacts(context); recents = loadRecents(context) }
    }
    LaunchedEffect(Unit) { ensurePermissions() }

    MaterialTheme(colorScheme = lightColorScheme(background = BgTop, surface = BgTop)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgTop, BgBottom)))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (tab == 0) "Phone" else if (tab == 1) "Recents" else "Contacts", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Spacer(Modifier.weight(1f))
                    Text("⋮", fontSize = 30.sp, color = Ink, modifier = Modifier.clickable { showDefaultPrompt = true })
                }
                AnimatedContent(targetState = tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { page ->
                    when (page) {
                        0 -> DialPage(number, { number = it }, context, contacts)
                        1 -> RecentsPage(recents) { dial(context, it.number) }
                        else -> ContactsPage(contacts) { dial(context, it.number) }
                    }
                }
                Spacer(Modifier.weight(1f))
                NavigationBar(containerColor = Glass, tonalElevation = 0.dp) {
                    NavItem("⌨", "Keypad", tab == 0) { tab = 0 }
                    NavItem("◷", "Recents", tab == 1) { tab = 1; ensurePermissions() }
                    NavItem("♙", "Contacts", tab == 2) { tab = 2; ensurePermissions() }
                }
            }
        }
    }
    if (showDefaultPrompt) AlertDialog(
        onDismissRequest = { showDefaultPrompt = false },
        title = { Text("Set as default Phone app?") },
        text = { Text("This enables the full Android calling experience, including incoming-call and in-call UI where supported.") },
        confirmButton = { TextButton(onClick = { showDefaultPrompt = false; onRequestDefaultDialer() }) { Text("Set default") } },
        dismissButton = { TextButton(onClick = { showDefaultPrompt = false }) { Text("Later") } }
    )
}

@Composable private fun NavItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { Text(icon, fontSize = 21.sp) }, label = { Text(label, fontSize = 11.sp) })
}

@Composable private fun DialPage(number: String, setNumber: (String) -> Unit, context: Context, contacts: List<ContactItem>) {
    val matches = if (number.length >= 2) contacts.filter { it.name.contains(number, true) || it.number.replace(" ", "").contains(number) }.take(3) else emptyList()
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        matches.forEach { c -> ContactSuggestion(c) { dial(context, c.number) }; Spacer(Modifier.height(6.dp)) }
        Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) { Text(number, fontSize = 32.sp, color = Ink, maxLines = 1) }
        val keys = listOf("1" to "", "2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL", "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "*" to "", "0" to "+", "#" to "")
        keys.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { (d, l) -> Key(d, l) { setNumber(number + d) } } }
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(72.dp))
            Box(Modifier.size(68.dp).background(Color(0xFF38B66A), CircleShape).clickable { if (number.isNotBlank()) dial(context, number) }, contentAlignment = Alignment.Center) { Text("☎", fontSize = 29.sp, color = Color.White) }
            Box(Modifier.width(72.dp).padding(start = 24.dp), contentAlignment = Alignment.Center) { if (number.isNotEmpty()) Text("⌫", fontSize = 25.sp, color = Ink, modifier = Modifier.clickable { setNumber(number.dropLast(1)) }) }
        }
    }
}

@Composable private fun Key(digit: String, letters: String, onClick: () -> Unit) {
    Column(Modifier.size(86.dp).background(Glass, RoundedCornerShape(30.dp)).clickable { onClick() }.padding(top = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(digit, fontSize = 29.sp, fontWeight = FontWeight.Medium, color = Ink)
        Text(letters, fontSize = 9.sp, letterSpacing = 2.sp, color = Muted, modifier = Modifier.offset(y = (-2).dp))
    }
}

@Composable private fun ContactSuggestion(c: ContactItem, onCall: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Glass, RoundedCornerShape(20.dp)).clickable { onCall() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(c.name); Column(Modifier.padding(start = 12.dp)) { Text(c.name, fontSize = 16.sp, color = Ink); Text(c.number, fontSize = 13.sp, color = Muted) }
    }
}

@Composable private fun RecentsPage(items: List<RecentItem>, onCall: (RecentItem) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        SearchBar("Search contacts & numbers"); Spacer(Modifier.height(18.dp))
        if (items.isEmpty()) RecentRow("No recent calls", "Your call history will appear here") else items.take(30).forEach { r -> RecentCallRow(r) { onCall(r) } }
    }
}

@Composable private fun ContactsPage(items: List<ContactItem>, onCall: (ContactItem) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        SearchBar("Search contacts"); Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) RecentRow("No contacts available", "Allow contacts permission to show people here") else items.take(100).forEach { c -> ContactSuggestion(c) { onCall(c) }; Spacer(Modifier.height(6.dp)) }
    }
}

@Composable private fun RecentCallRow(r: RecentItem, onCall: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onCall() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(r.name)
        Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(r.name, fontSize = 16.sp, color = Ink); Text(r.number + "  •  " + r.time, fontSize = 13.sp, color = Muted) }
        Text("☎", color = Color(0xFF2E9F5B), fontSize = 22.sp)
    }
}

@Composable private fun Avatar(name: String) { Box(Modifier.size(48.dp).background(Color(0xFFE0E2DF), CircleShape), contentAlignment = Alignment.Center) { Text(name.firstOrNull()?.uppercase() ?: "•", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium) } }
@Composable private fun SearchBar(text: String) { Box(Modifier.fillMaxWidth().height(52.dp).background(Glass, RoundedCornerShape(28.dp)), contentAlignment = Alignment.CenterStart) { Text("⌕   $text", color = Muted, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 18.dp)) } }
@Composable private fun RecentRow(title: String, subtitle: String) { Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).background(Color(0xFFE1E2DF), CircleShape), contentAlignment = Alignment.Center) { Text("•", color = Muted, fontSize = 24.sp) }; Column(Modifier.padding(start = 14.dp)) { Text(title, fontSize = 17.sp, color = Ink); Text(subtitle, fontSize = 13.sp, color = Muted) } } }

private fun dial(context: Context, number: String) {
    val uri = Uri.parse("tel:" + Uri.encode(number))
    context.startActivity(Intent(Intent.ACTION_CALL, uri))
}

private fun loadContacts(context: Context): List<ContactItem> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return emptyList()
    val out = mutableListOf<ContactItem>()
    context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE")?.use { c -> while (c.moveToNext()) out += ContactItem(c.getString(0) ?: "Unknown", c.getString(1) ?: "") }
    return out.distinctBy { it.number }
}

private fun loadRecents(context: Context): List<RecentItem> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return emptyList()
    val out = mutableListOf<RecentItem>()
    context.contentResolver.query(CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE), null, null, CallLog.Calls.DATE + " DESC")?.use { c -> while (c.moveToNext() && out.size < 50) { val number = c.getString(0) ?: ""; val name = c.getString(1) ?: number; val type = c.getInt(2); val time = android.text.format.DateUtils.getRelativeTimeSpanString(c.getLong(3), System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS).toString(); out += RecentItem(name, number, type, time) } }
    return out
}
