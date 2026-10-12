package com.example.evaluacion2iot

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

@Composable
fun LoginScreen(onLoginExitoso: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var puedeReenviar by remember { mutableStateOf(false) }

    fun validar(): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()) {
            error = "Ingresa un correo válido"
            return false
        }
        if (clave.length < 6) {
            error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }
        return true
    }

    fun traducir(e: Exception?): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
        is FirebaseAuthInvalidUserException -> "Esa cuenta no existe"
        is FirebaseAuthUserCollisionException -> "Ese correo ya está registrado"
        else -> "No se pudo conectar. Revisa tu internet"
    }

    fun iniciarSesion() {
        error = null
        aviso = null
        puedeReenviar = false
        cargando = true
        auth.signInWithEmailAndPassword(correo.trim(), clave)
            .addOnCompleteListener { t ->
                if (!t.isSuccessful) {
                    cargando = false
                    error = traducir(t.exception)
                    return@addOnCompleteListener
                }
                // Se recarga el usuario para leer si ya verificó el correo
                auth.currentUser?.reload()?.addOnCompleteListener {
                    cargando = false
                    if (auth.currentUser?.isEmailVerified == true) {
                        onLoginExitoso()
                    } else {
                        error = "Debes verificar tu correo antes de entrar"
                        puedeReenviar = true
                    }
                }
            }
    }

    fun crearCuenta() {
        error = null
        aviso = null
        puedeReenviar = false
        cargando = true
        auth.createUserWithEmailAndPassword(correo.trim(), clave)
            .addOnCompleteListener { t ->
                if (!t.isSuccessful) {
                    cargando = false
                    error = traducir(t.exception)
                    return@addOnCompleteListener
                }
                auth.currentUser?.sendEmailVerification()
                    ?.addOnCompleteListener { envio ->
                        cargando = false
                        auth.signOut()
                        aviso = if (envio.isSuccessful) {
                            "Te enviamos un correo de verificación a ${correo.trim()}. " +
                                    "Ábrelo, toca el enlace (revisa también spam) y luego inicia sesión."
                        } else {
                            "Cuenta creada, pero no se pudo enviar el correo. " +
                                    "Inicia sesión para reenviarlo."
                        }
                    }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Evaluación 2 IoT", style = MaterialTheme.typography.headlineMedium)
        Text("Inicia sesión para continuar", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        aviso?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        if (puedeReenviar) {
            TextButton(onClick = {
                auth.currentUser?.sendEmailVerification()
                    ?.addOnCompleteListener { envio ->
                        if (envio.isSuccessful) {
                            error = null
                            aviso = "Correo reenviado. Revisa tu bandeja y spam."
                        } else {
                            error = "No se pudo reenviar. Espera un momento e intenta de nuevo"
                        }
                    }
            }) { Text("Reenviar correo de verificación") }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { if (validar()) iniciarSesion() },
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Iniciar sesión") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { if (validar()) crearCuenta() },
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Crear cuenta") }
    }
}