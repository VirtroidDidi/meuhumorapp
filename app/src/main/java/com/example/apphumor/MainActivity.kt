package com.example.apphumor

import android.animation.LayoutTransition
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.apphumor.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity(), ProfileFragment.LogoutListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false

    // Lançador de permissão para Notificações (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 1. Usuário aceitou
            android.util.Log.d("PermissaoNotificacao", "Permissão CONCEDIDA pelo usuário.")
            // Opcional: Se quiser dar um feedback visual sutil
            // Toast.makeText(this, "Lembretes ativados!", Toast.LENGTH_SHORT).show()
        } else {
            // 2. Usuário negou
            android.util.Log.w("PermissaoNotificacao", "Permissão NEGADA pelo usuário.")

            // DICA DE UX (Pode implementar no futuro):
            // Aqui seria o lugar ideal para mostrar um Dialog explicando:
            // "O AppHumor precisa de notificações para te lembrar de registrar seu dia.
            // Vá em Configurações para ativar."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.layoutTransition = LayoutTransition()
        auth = FirebaseAuth.getInstance()

        setupNavigation()
        setupConnectionMonitor()
        askNotificationPermission()
    }

    private fun setupNavigation() {
        // Encontra o NavHostFragment que definimos no XML
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerMain) as NavHostFragment

        // Pega o controlador de navegação
        val navController = navHostFragment.navController

        // Conecta a BottomNavigationView com o NavController
        // A mágica acontece aqui: como os IDs do menu batem com o gráfico,
        // ele navega automaticamente!
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun askNotificationPermission() {
        // Essa permissão só existe no Android 13 (API 33) ou superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS

            // Verifica se JÁ temos a permissão
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("PermissaoNotificacao", "Permissão já garantida anteriormente.")
            } else {
                // Se não temos, pedimos ao sistema
                // O sistema decide se mostra o popup ou se o usuário já negou permanentemente
                requestPermissionLauncher.launch(permission)
            }
        } else {
            android.util.Log.d("PermissaoNotificacao", "Android < 13: Permissão concedida automaticamente na instalação.")
        }
    }

    private fun setupConnectionMonitor() {
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                isConnected = connected
                handler.post { updateConnectionUI(connected) }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateConnectionUI(isConnected: Boolean) {
        if (isConnected) {
            binding.cvConnectionStatus.visibility = View.GONE
        } else {
            binding.cvConnectionStatus.visibility = View.VISIBLE
            binding.cvConnectionStatus.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.status_offline_bg)
            )
            binding.tvStatusText.text = getString(R.string.status_offline)
            binding.ivStatusIcon.setImageResource(R.drawable.ic_cloud_off_24)
        }
    }

    fun isNetworkConnected(): Boolean {
        return isConnected
    }

    override fun onLogoutSuccess() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}