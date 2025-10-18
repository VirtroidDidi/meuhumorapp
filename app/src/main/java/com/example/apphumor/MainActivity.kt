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
    // Habilitando FragmentTelaB (Tela Usuário)
    private val fragmentTelaB = FragmentTelaB()
    private val fragmentTelaC = FragmentTelaC()
    private var activeFragment: Fragment? = null
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate( layoutInflater )
        setContentView(binding. root )

        auth = FirebaseAuth.getInstance()

        // Configuração inicial sem adicionar fragments
        if (savedInstanceState == null) {
            showInitialFragment()
        }

        setupNavigationButtons()
        // O botão de Logout foi removido do XML, então a função setupLogoutButton() foi removida.
    }

    private fun showInitialFragment() {
        supportFragmentManager .beginTransaction()
            // Adiciona a Tela A (Hoje)
            .add(R.id. fragmentContainerMain ,  fragmentTelaA ,  "fragment_a")
            // Adiciona a Tela B (Usuário)
            .add(R.id. fragmentContainerMain ,  fragmentTelaB ,  "fragment_b")
            // Adiciona a Tela C (Histórico)
            .add(R.id. fragmentContainerMain ,  fragmentTelaC ,  "fragment_c")
            // Esconde as telas B e C, deixando A como ativa
            .hide(fragmentTelaB)
            .hide(fragmentTelaC)
            .commit()

        activeFragment = fragmentTelaA
    }

    private fun setupNavigationButtons() {
        // Botão HOJE (FragmentTelaA)
        binding.btnHoje.setOnClickListener  {
            if (activeFragment != fragmentTelaA) {
                showFragment(fragmentTelaA)
            }
        }

        // NOVO: Botão USUÁRIO (FragmentTelaB)
        binding.btnUsuario.setOnClickListener  {
            if (activeFragment != fragmentTelaB) {
                showFragment(fragmentTelaB)
            }
        }

        // Botão HISTÓRICO (FragmentTelaC)
        binding.btnHistorico.setOnClickListener  {
            if (activeFragment != fragmentTelaC) {
                showFragment(fragmentTelaC)
            }
        }
    }

    // A função setupLogoutButton foi REMOVIDA.

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager .beginTransaction(). apply  {
            activeFragment?. let  {  hide( it )  }

            if (fragment. isAdded ) {
                show(fragment)
            } else {
                add(R.id. fragmentContainerMain ,  fragment ,  fragment. tag )
            }

            commit()
        }
        activeFragment = fragment
    }
}
