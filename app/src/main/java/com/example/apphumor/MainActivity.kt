package com.example.apphumor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.animation.LayoutTransition
import androidx.activity.result.contract.ActivityResultContracts // Importante
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
    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val historyFragment = HistoryFragment()
    private var activeFragment: Fragment? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false

    // 1. Criar o lançador de permissão
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permissão concedida
        } else {
            // Permissão negada. O app funcionará sem notificações.
        }
    }

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

        // 2. Chamar a verificação de permissão
        askNotificationPermission()
    }

    // Função para pedir permissão no Android 13+
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ... (Mantenha o restante do código: showInitialFragment, setupNavigationButtons, etc.) ...

    private fun showInitialFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainerMain, homeFragment, "fragment_a")
            .add(R.id.fragmentContainerMain, profileFragment, "fragment_b")
            .add(R.id.fragmentContainerMain, historyFragment, "fragment_c")
            .hide(profileFragment)
            .hide(historyFragment)
            .commit()
        activeFragment = homeFragment
    }

    private fun setupNavigationButtons() {
        binding.btnHoje.setOnClickListener {
            if (activeFragment != homeFragment) showFragment(homeFragment)
        }
        binding.btnUsuario.setOnClickListener {
            if (activeFragment != profileFragment) showFragment(profileFragment)
        }
        binding.btnHistorico.setOnClickListener {
            if (activeFragment != historyFragment) showFragment(historyFragment)
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
            binding.tvStatusText.text = "Você está offline. Alterações salvas localmente."
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