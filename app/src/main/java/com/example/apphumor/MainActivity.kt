package com.example.apphumor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.apphumor.databinding.ActivityMainBinding
import com.example.apphumor.viewmodel.LoginActivity
import com.google.firebase.auth.FirebaseAuth

// ===================================
// NOVOS IMPORTS
// ===================================
import android.view.View
import android.animation.LayoutTransition
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import android.os.Handler
import android.os.Looper
// ===================================

// CORREÇÃO: A classe agora implementa a interface FragmentTelaB.LogoutListener
class MainActivity : AppCompatActivity(), ProfileFragment.LogoutListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()
    private val historyFragment = HistoryFragment()
    private var activeFragment: Fragment? = null
    //private val TAG = "MainActivity"

    // ===================================
    // NOVAS VARIÁVEIS DE CONEXÃO
    // ===================================
    private val handler = Handler(Looper.getMainLooper())
    private var isConnected = false
    // ===================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate( layoutInflater )
        setContentView(binding. root )

        // ===================================
        // NOVO: Habilita animação suave de layout
        // ===================================
        binding.root.layoutTransition = LayoutTransition()

        auth = FirebaseAuth.getInstance()

        // Configuração inicial sem adicionar fragments
        if (savedInstanceState == null) {
            showInitialFragment()
        }

        setupNavigationButtons()

        // ===================================
        // NOVO: Iniciar monitoramento de conexão
        // ===================================
        setupConnectionMonitor()
        // O botão de Logout foi removido do XML, então a função setupLogoutButton() foi removida.
    }

    private fun showInitialFragment() {
        supportFragmentManager .beginTransaction()
            // Adiciona a Tela A (Hoje)
            .add(R.id. fragmentContainerMain ,  homeFragment ,  "fragment_a")
            // Adiciona a Tela B (Usuário)
            .add(R.id. fragmentContainerMain ,  profileFragment ,  "fragment_b")
            // Adiciona a Tela C (Histórico)
            .add(R.id. fragmentContainerMain ,  historyFragment ,  "fragment_c")
            // Esconde as telas B e C, deixando A como ativa
            .hide(profileFragment)
            .hide(historyFragment)
            .commit()

        activeFragment = homeFragment
    }

    private fun setupNavigationButtons() {
        // Botão HOJE (FragmentTelaA)
        binding.btnHoje.setOnClickListener  {
            if (activeFragment != homeFragment) {
                showFragment(homeFragment)
            }
        }

        // NOVO: Botão USUÁRIO (FragmentTelaB)
        binding.btnUsuario.setOnClickListener  {
            if (activeFragment != profileFragment) {
                showFragment(profileFragment)
            }
        }

        // Botão HISTÓRICO (FragmentTelaC)
        binding.btnHistorico.setOnClickListener  {
            if (activeFragment != historyFragment) {
                showFragment(historyFragment)
            }
        }
    }

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

    // ===================================
    // NOVAS FUNÇÕES DE MONITORAMENTO DE CONEXÃO
    // ===================================
    private fun setupConnectionMonitor() {
        // Referência especial do Firebase para saber o estado da conexão
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                isConnected = connected

                // Usamos o Handler para evitar updates de UI muito rápidos (flickering)
                handler.post { updateConnectionUI(connected) }
            }

            override fun onCancelled(error: DatabaseError) {
                // Não é crítico tratar erro de listener de .info
            }
        })
    }

    private fun updateConnectionUI(isConnected: Boolean) {
        if (isConnected) {
            // Se voltou a conexão, esconde o banner
            binding.cvConnectionStatus.visibility = View.GONE
        } else {
            // Se caiu, mostra o banner e ajusta cores/texto
            binding.cvConnectionStatus.visibility = View.VISIBLE
            binding.cvConnectionStatus.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.status_offline_bg)
            )
            binding.tvStatusText.text = "Você está offline. Alterações salvas localmente."
            binding.ivStatusIcon.setImageResource(R.drawable.ic_cloud_off_24)
            // Os atributos de cor do texto e do ícone já foram definidos no XML,
            // mas é bom garantir que estejam corretos aqui, se necessário:
            // binding.tvStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_offline_text))
            // binding.ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_offline_text))
        }
    }

    /**
     * Método público para outras Activities/Fragments saberem o status (usado na Tarefa 2.4)
     */
    fun isNetworkConnected(): Boolean {
        return isConnected
    }
    // ===================================

    /**
     * Implementação do método da interface FragmentTelaB.LogoutListener.
     * Este código é executado quando o usuário clica em "Sair da Conta" no FragmentTelaB.
     */
    override fun onLogoutSuccess() {
        // Redireciona para a tela de Login
        val intent = Intent(this, LoginActivity::class.java).apply {
            // Estas flags garantem que o usuário não possa voltar para a MainActivity
            // após o logout (limpa o histórico de Activities)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Finaliza a MainActivity para que o usuário não possa voltar
    }
}