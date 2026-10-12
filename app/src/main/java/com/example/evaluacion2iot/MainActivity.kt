package com.example.evaluacion2iot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.evaluacion2iot.ui.theme.Evaluacion2IoTTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Evaluacion2IoTTheme {
                val navController = rememberNavController()
                val inicio = if (FirebaseAuth.getInstance().currentUser?.isEmailVerified == true) "color" else "login"

                NavHost(navController = navController, startDestination = inicio) {
                    composable("login") {
                        LoginScreen(onLoginExitoso = {
                            navController.navigate("color") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }
                    composable("color") {
                        ColorScreen(onCerrarSesion = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("color") { inclusive = true }
                            }
                        })
                    }
                }
            }
        }
    }
}