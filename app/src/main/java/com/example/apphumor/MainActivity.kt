package com.example.apphumor

import android.Manifest
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
import androidx.fragment.app.Fragment
import com.example.apphumor.databinding.ActivityMainBinding
import com.example.apphumor.viewmodel.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity(), ProfileFragment.LogoutListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    // Fragmentos instanciados
    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val historyFragment = HistoryFragment()

    private var activeFragment: Fragment? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false

    // Lançador de permissão para Notificações (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.layoutTransition = LayoutTransition()
        auth = FirebaseAuth.getInstance()

        if (savedInstanceState == null) {
            showInitialFragment()
        }

        setupNavigationButtons()
        setupConnectionMonitor()
        askNotificationPermission()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showInitialFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainerMain, homeFragment, "fragment_home")
            .add(R.id.fragmentContainerMain, profileFragment, "fragment_profile")
            .add(R.id.fragmentContainerMain, historyFragment, "fragment_history")
            .hide(profileFragment)
            .hide(historyFragment)
            .commit()
        activeFragment = homeFragment

        // Garante que o botão inicial esteja visualmente selecionado na cor correta
        updateNavigationState(R.id.btn_hoje)
    }

    private fun setupNavigationButtons() {
        binding.btnHoje.setOnClickListener {
            if (activeFragment != homeFragment) {
                showFragment(homeFragment)
                updateNavigationState(R.id.btn_hoje)
            }
        }
        binding.btnUsuario.setOnClickListener {
            if (activeFragment != profileFragment) {
                showFragment(profileFragment)
                updateNavigationState(R.id.btn_usuario)
            }
        }
        binding.btnHistorico.setOnClickListener {
            if (activeFragment != historyFragment) {
                showFragment(historyFragment)
                updateNavigationState(R.id.btn_historico)
            }
        }
    }

    // --- NOVA FUNÇÃO: Gerencia a lógica visual dos botões ---
    private fun updateNavigationState(activeButtonId: Int) {
        // Lista com todos os botões da barra
        val buttons = listOf(binding.btnHoje, binding.btnHistorico, binding.btnUsuario)

        buttons.forEach { button ->
            val isActive = button.id == activeButtonId
            // Define o estado 'checked'. O XML nav_item_colors usará isso para trocar a cor.
            button.isChecked = isActive

            // Opcional: Impede clicar no botão que já está ativo
            button.isClickable = !isActive
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            activeFragment?.let { hide(it) }
            if (fragment.isAdded) {
                show(fragment)
            } else {
                add(R.id.fragmentContainerMain, fragment, fragment.tag)
            }
            commit()
        }
        activeFragment = fragment
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