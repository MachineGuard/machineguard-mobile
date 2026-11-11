package com.machineguard.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.machineguard.mobile.boundedcontexts.iam.infrastructure.IamGraph
import com.machineguard.mobile.boundedcontexts.iam.presentation.AuthGate
import com.machineguard.mobile.core.theme.MachineGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IamGraph.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MachineGuardTheme {
                AuthGate {
                    AppShell()
                }
            }
        }
    }
}
