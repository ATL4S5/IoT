package com.example.evaluacion2iot

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val colores = mapOf(
    "VERDE" to Color(0xFF2E7D32),
    "AMARILLO" to Color(0xFFF9A825),
    "ROJO" to Color(0xFFC62828)
)

data class Cambio(val color: String, val usuario: String, val fecha: Long)

@Composable
fun ColorScreen(onCerrarSesion: () -> Unit) {
    val db = remember { FirebaseDatabase.getInstance().reference }
    val usuario = FirebaseAuth.getInstance().currentUser?.email ?: "desconocido"

    var colorActual by remember { mutableStateOf("VERDE") }
    var cambiadoPor by remember { mutableStateOf("") }
    var historial by remember { mutableStateOf(listOf<Cambio>()) }

    // Escucha en tiempo real: cuando otro celular cambia el color, se actualiza aquí
    DisposableEffect(Unit) {
        val refEstado = db.child("estado")
        val escuchaEstado = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                colorActual = s.child("color").getValue(String::class.java) ?: "VERDE"
                cambiadoPor = s.child("cambiadoPor").getValue(String::class.java) ?: ""
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        refEstado.addValueEventListener(escuchaEstado)

        val consultaHistorial = db.child("historial").limitToLast(10)
        val escuchaHistorial = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                historial = s.children.mapNotNull { c ->
                    val col = c.child("color").getValue(String::class.java)
                        ?: return@mapNotNull null
                    Cambio(
                        col,
                        c.child("usuario").getValue(String::class.java) ?: "",
                        c.child("fecha").getValue(Long::class.java) ?: 0L
                    )
                }.reversed()
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        consultaHistorial.addValueEventListener(escuchaHistorial)

        onDispose {
            refEstado.removeEventListener(escuchaEstado)
            consultaHistorial.removeEventListener(escuchaHistorial)
        }
    }

    fun cambiarColor(nombre: String) {
        val estado = mapOf(
            "color" to nombre,
            "cambiadoPor" to usuario,
            "fecha" to ServerValue.TIMESTAMP
        )
        val registro = mapOf(
            "color" to nombre,
            "usuario" to usuario,
            "fecha" to ServerValue.TIMESTAMP
        )
        db.child("estado").setValue(estado)
        db.child("historial").push().setValue(registro)
    }

    val fondo by animateColorAsState(colores[colorActual] ?: Color.Gray, label = "fondo")
    val textoColor = if (colorActual == "AMARILLO") Color.Black else Color.White
    val formato = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fondo)
            .safeDrawingPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Color actual: $colorActual", style = MaterialTheme.typography.headlineMedium, color = textoColor)
        if (cambiadoPor.isNotEmpty()) {
            Text("Cambiado por: $cambiadoPor", color = textoColor)
        }
        Text("Sesión: $usuario", color = textoColor, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(8.dp))
        Text("Elige un color:", color = textoColor, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            colores.forEach { (nombre, color) ->
                Button(
                    onClick = { cambiarColor(nombre) },
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    border = BorderStroke(2.dp, Color.White),
                    modifier = Modifier.weight(1f)
                ) { Text(nombre, color = if (nombre == "AMARILLO") Color.Black else Color.White) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Historial (últimos 10)", color = textoColor, style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (historial.isEmpty()) Text("Aún no hay cambios")
                historial.forEach {
                    Text("${formato.format(Date(it.fecha))} · ${it.color} · ${it.usuario}")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onCerrarSesion,
            border = BorderStroke(1.dp, Color.White),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Cerrar sesión", color = textoColor) }
    }
}