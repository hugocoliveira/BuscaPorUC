package br.com.lit.busca.uc.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ---------------------------------------------------------------------------
// ViewModel da tela de leitura de QR Code do BuscaPorUC.
// ---------------------------------------------------------------------------

/**
 * Rótulos dos campos na ordem exata em que aparecem no QR Code de UC.
 * Formato do QR: TD;ITD;Tds;ST.;TpPr;PArm;Prod;PDO;PDD;DtCo;HrCo
 */
private val ROTULOS_CAMPOS = listOf(
    "TD", "ITD", "Tds", "ST.", "TpPr", "PArm", "Prod", "PDO", "PDD", "DtCo", "HrCo"
)

/**
 * Estado imutável da tela de leitura.
 *
 * @property campos        pares (rótulo, valor) extraídos do QR. null antes do primeiro scan.
 * @property scannerAberto true quando o overlay de câmera está visível.
 * @property erro          mensagem de erro ou null.
 */
data class UiState(
    val campos: List<Pair<String, String>>? = null,
    val scannerAberto: Boolean              = false,
    val erro: String?                       = null
)

/**
 * ViewModel que gerencia o estado e os eventos da tela [MainScreen].
 */
class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Chamado quando a câmera detecta um QR Code.
     * Faz o parse por ';' e mapeia para os rótulos definidos em [ROTULOS_CAMPOS].
     *
     * @param codigo valor bruto lido (ex: "100003904;1;1;C;Y353;YINQ;EWMS4-03;...").
     */
    fun onCodigoEscaneado(codigo: String) {
        val partes = codigo.split(";")

        if (partes.size != ROTULOS_CAMPOS.size) {
            _uiState.update {
                it.copy(
                    scannerAberto = false,
                    erro   = "QR Code inválido: esperado ${ROTULOS_CAMPOS.size} campos, recebido ${partes.size}.",
                    campos = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                scannerAberto = false,
                campos        = ROTULOS_CAMPOS.zip(partes),
                erro          = null
            )
        }
    }

    /** Abre o overlay da câmera para uma nova leitura. */
    fun onAbrirScanner() {
        _uiState.update { it.copy(scannerAberto = true, erro = null) }
    }

    /** Fecha o overlay da câmera sem alterar os dados exibidos. */
    fun onFecharScanner() {
        _uiState.update { it.copy(scannerAberto = false) }
    }
}
