package com.raj.realmedialer

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.getSystemService
import com.raj.realmedialer.ui.DialerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DialerApp(onRequestDefaultDialer = ::requestDefaultDialer) }
    }

    private fun requestDefaultDialer() {
        val roleManager = getSystemService<RoleManager>() ?: return
        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER), 42)
        }
    }
}
