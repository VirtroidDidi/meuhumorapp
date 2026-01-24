package com.example.apphumor.models

import com.example.apphumor.R
import org.junit.Assert.assertEquals
import org.junit.Test

class HumorTypeTest {

    @Test
    fun `fromKey deve retornar RAD quando receber chave legacy Incrivel`() {
        // Cenário: Banco de dados antigo com string em português
        val input = "Incrível"

        // Ação
        val result = HumorType.fromKey(input)

        // Verificação
        assertEquals(HumorType.RAD, result)
    }

    @Test
    fun `fromKey deve retornar HAPPY quando receber chave legacy Bem`() {
        val input = "Bem"
        val result = HumorType.fromKey(input)
        assertEquals(HumorType.HAPPY, result)
    }

    @Test
    fun `fromKey deve retornar NEUTRAL quando receber string desconhecida ou nula`() {
        assertEquals(HumorType.NEUTRAL, HumorType.fromKey("Abobrinha"))
        assertEquals(HumorType.NEUTRAL, HumorType.fromKey(null))
        assertEquals(HumorType.NEUTRAL, HumorType.fromKey(""))
    }

    @Test
    fun `fromKey deve ser case insensitive`() {
        assertEquals(HumorType.RAD, HumorType.fromKey("rad"))
        assertEquals(HumorType.RAD, HumorType.fromKey("RAD"))
    }

    @Test
    fun `fromChipId deve retornar o tipo correto`() {
        // Verifica se o ID do chip da UI mapeia para o Enum certo
        assertEquals(HumorType.SAD, HumorType.fromChipId(R.id.chip_sad))
        assertEquals(null, HumorType.fromChipId(99999)) // ID inexistente
    }
}