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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // Importação Crucial
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

    // Variável para controlar se o app está pronto para exibir a UI
    private var isReady = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("PermissaoNotificacao", "Permissão CONCEDIDA pelo usuário.")
        } else {
            android.util.Log.w("PermissaoNotificacao", "Permissão NEGADA pelo usuário.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Instala a Splash Screen (DEVE ser a primeira linha)
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Inicializa Auth
        auth = FirebaseAuth.getInstance()

        // 2. Lógica de Roteamento (Opção B)
        // Verificamos se tem usuário ANTES de carregar o layout pesado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Se NÃO estiver logado, manda pro Login e mata a Main
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return // Para a execução aqui
        }

        // Se chegou aqui, o usuário está logado.
        // Podemos liberar a Splash Screen imediatamente ou carregar algo.
        // Como o Firebase User já está em memória, liberamos o app.
        isReady = true

        // 3. (Opcional) Segura a Splash até isReady ser true
        splashScreen.setKeepOnScreenCondition {
            !isReady
        }

        // Carrega a UI Normal da MainActivity
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.layoutTransition = LayoutTransition()

        setupNavigation()
        setupConnectionMonitor()
        askNotificationPermission()
    }

    // ... Restante do código (setupNavigation, askNotificationPermission, etc) mantém igual ...

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerMain) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("PermissaoNotificacao", "Permissão já garantida anteriormente.")
            } else {
                requestPermissionLauncher.launch(permission)
            }
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