package br.com.lit.busca.uc.ui

import android.Manifest
import android.media.MediaPlayer
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.lit.busca.uc.R
import br.com.lit.busca.uc.scanner.iniciarScanner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.ExecutorService

private const val TAG = "MainScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context         = LocalContext.current
    val lifecycleOwner  = LocalLifecycleOwner.current
    val permissaoCamera = rememberPermissionState(Manifest.permission.CAMERA)

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text(stringResource(R.string.titulo_tela), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ConteudoPrincipal(
                uiState         = uiState,
                onCampoAlterado = viewModel::onCampoAlterado,
                onBuscar        = viewModel::onBuscar,
                onLimpar        = viewModel::onLimpar,
                onAbrirScanner  = {
                    if (permissaoCamera.status.isGranted) viewModel.onAbrirScanner()
                    else permissaoCamera.launchPermissionRequest()
                }
            )
            AnimatedVisibility(visible = uiState.scannerAberto && permissaoCamera.status.isGranted, enter = fadeIn(), exit = fadeOut()) {
                ScannerOverlay(onCodigoLido = viewModel::onCodigoEscaneado, onFechar = viewModel::onFecharScanner, lifecycleOwner = lifecycleOwner, context = context)
            }
            if (uiState.scannerAberto && !permissaoCamera.status.isGranted) permissaoCamera.launchPermissionRequest()
        }
    }
}

@Composable
private fun ConteudoPrincipal(
    uiState: UiState,
    onCampoAlterado: (String) -> Unit,
    onBuscar: () -> Unit,
    onLimpar: () -> Unit,
    onAbrirScanner: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Foca o campo ao abrir o app — cursor pronto para bipar sem abrir o teclado virtual
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
        kotlinx.coroutines.delay(100)
        keyboardController?.hide()
    }

    LaunchedEffect(uiState.campos) {
        if (!uiState.campos.isNullOrEmpty()) {
            runCatching { focusRequester.requestFocus() }
            kotlinx.coroutines.delay(100)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(uiState.erro) {
        if (uiState.erro != null) {
            val mp = MediaPlayer.create(context, R.raw.error)
            mp.setOnCompletionListener { it.release() }
            mp.start()
            runCatching { focusRequester.requestFocus() }
            kotlinx.coroutines.delay(100)
            keyboardController?.hide()
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {

        // Campo de texto + botão Buscar
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value         = uiState.campoBusca,
                    onValueChange = onCampoAlterado,
                    modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    label         = { Text(stringResource(R.string.label_campo_busca)) },
                    placeholder   = { Text(stringResource(R.string.placeholder_campo_busca)) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    trailingIcon  = {
                        if (uiState.campoBusca.isNotEmpty()) {
                            IconButton(onClick = onLimpar) {
                                Icon(Icons.Default.Clear, stringResource(R.string.botao_limpar), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            IconButton(onClick = onAbrirScanner) {
                                Icon(Icons.Default.QrCodeScanner, stringResource(R.string.descricao_icone_scanner), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onBuscar() })
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick  = onBuscar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = uiState.campoBusca.isNotBlank(),
                    shape    = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.botao_buscar), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Banner de erro
        if (uiState.erro != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    shape    = RoundedCornerShape(6.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(uiState.erro, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }
        }

        // Card de resultados — grid 2 colunas
        if (!uiState.campos.isNullOrEmpty()) {
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    shape     = RoundedCornerShape(6.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier            = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.campos.chunked(2).forEach { par ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                CampoLinha(par[0].first, par[0].second, Modifier.weight(1f))
                                Spacer(Modifier.width(16.dp))
                                if (par.size > 1) CampoLinha(par[1].first, par[1].second, Modifier.weight(1f))
                                else Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Estado inicial — nenhum dado lido ainda
        if (uiState.campos == null && uiState.erro == null && uiState.campoBusca.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(64.dp).height(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.aguardando_leitura), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun CampoLinha(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("$rotulo:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(valor, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ScannerOverlay(onCodigoLido: (String) -> Unit, onFechar: () -> Unit, lifecycleOwner: androidx.lifecycle.LifecycleOwner, context: android.content.Context) {
    val executorRef = remember { mutableListOf<ExecutorService>() }
    DisposableEffect(Unit) { onDispose { executorRef.firstOrNull()?.shutdown(); executorRef.clear(); Log.d(TAG, "Executor encerrado.") } }
    val jaLeu = remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            val pv = PreviewView(ctx)
            executorRef.add(iniciarScanner(context, lifecycleOwner, pv) { codigo -> if (!jaLeu.value) { jaLeu.value = true; onCodigoLido(codigo) } })
            pv
        }, modifier = Modifier.fillMaxSize())
        IconButton(onClick = onFechar, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Icon(Icons.Default.Close, stringResource(R.string.fechar_scanner), tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
