package com.example.apphumor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.apphumor.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val fragmentTelaA = FragmentTelaA()
    private val fragmentTelaB = FragmentTelaB()
    private val fragmentTelaC = FragmentTelaC()
    private var activeFragment: Fragment? = null
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Configuração inicial sem adicionar fragments
        if (savedInstanceState == null) {
            showInitialFragment()
        }

        setupNavigationButtons()
        setupLogoutButton()
    }

    private fun showInitialFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainerMain, fragmentTelaA, "fragment_a")
            .add(R.id.fragmentContainerMain, fragmentTelaB, "fragment_b")
            .add(R.id.fragmentContainerMain, fragmentTelaC, "fragment_c")
            .hide(fragmentTelaB)
            .hide(fragmentTelaC)
            .commit()

        activeFragment = fragmentTelaA
    }

    private fun setupNavigationButtons() {
        binding.btnHoje.setOnClickListener {
            if (activeFragment != fragmentTelaA) {
                showFragment(fragmentTelaA)
            }
        }

        binding.btnCalendario.setOnClickListener {
            if (activeFragment != fragmentTelaB) {
                showFragment(fragmentTelaB)
            }
        }

        binding.btnHistorico.setOnClickListener {
            if (activeFragment != fragmentTelaC) {
                showFragment(fragmentTelaC)
            }
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
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
}