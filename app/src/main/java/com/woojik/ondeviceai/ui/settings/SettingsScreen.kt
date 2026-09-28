package com.woojik.ondeviceai.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woojik.ondeviceai.OnDeviceAiApplication
import com.woojik.ondeviceai.data.model.DarkThemeMode
import com.woojik.ondeviceai.data.model.ModelBackend

/** 설정 화면: 테마 모드 선택, 캐릭터 이름 변경, 로컬 모델 백엔드 선택/다운로드/가져오기 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locator = (context.applicationContext as OnDeviceAiApplication).locator
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(locator))
    val settings by viewModel.settings.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val selectedBackend by viewModel.selectedBackend.collectAsState()
    val crashLog by viewModel.crashLog.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importModel(uri)
    }

    var nameInput by remember { mutableStateOf("") }
    LaunchedEffect(settings.characterName) {
        nameInput = settings.characterName
    }
    // 화면에 다시 들어오면 크래시 로그를 갱신한다.
    LaunchedEffect(Unit) { viewModel.refreshCrashLog() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("테마", style = MaterialTheme.typography.titleLarge)
            DarkThemeMode.entries.forEach { mode ->
                Button(
                    onClick = { viewModel.setDarkThemeMode(mode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = settings.darkThemeMode != mode,
                ) {
                    Text(mode.label())
                }
            }

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("캐릭터 이름") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                singleLine = true,
            )
            Button(
                onClick = { viewModel.setCharacterName(nameInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = nameInput.isNotBlank(),
            ) {
                Text("저장")
            }

            Text(
                text = "로컬 모델",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp),
            )

            Text(
                text = "실행 백엔드 (Gemma 4 E2B 기준)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            ModelBackend.entries.forEach { backend ->
                OutlinedButton(
                    onClick = { viewModel.setModelBackend(backend) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = selectedBackend != backend,
                ) {
                    Text(backend.label())
                }
            }
            Text(
                text = "백엔드마다 모델 파일이 달라요. 전환 후에는 모델을 다시 다운로드해 주세요.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            when (val state = modelState) {
                is SettingsViewModel.ModelState.Active -> Text(
                    text = if (state.isLocal) {
                        "사용 중: " + state.modelName
                    } else {
                        "로컬 모델 없음 — 개발용 에코 모델 사용 중"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                is SettingsViewModel.ModelState.Importing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Text(
                        text = "모델 복사 중이에요. 파일이 클 경우 몇 분 걸릴 수 있어요.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is SettingsViewModel.ModelState.Downloading -> {
                    if (state.progressPercent != null) {
                        LinearProgressIndicator(
                            progress = { state.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                        Text(
                            text = "모델 다운로드 중이에요... " + state.progressPercent + "%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                        Text(
                            text = "모델 다운로드 중이에요. (Wi-Fi 권장)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.cancelDownload() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text("다운로드 취소")
                    }
                }
                is SettingsViewModel.ModelState.Failed -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Button(
                onClick = { viewModel.downloadModel() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = modelState !is SettingsViewModel.ModelState.Downloading &&
                    modelState !is SettingsViewModel.ModelState.Importing,
            ) {
                val target = com.woojik.ondeviceai.data.local.ModelDownloader.defaultModelFor(
                    useGpu = selectedBackend == ModelBackend.GPU,
                )
                Text("모델 자동 다운로드 (Gemma 4 E2B, " + target.sizeGb + ")")
            }
            Button(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                enabled = modelState !is SettingsViewModel.ModelState.Downloading &&
                    modelState !is SettingsViewModel.ModelState.Importing,
            ) {
                Text("파일로 직접 가져오기 (.litertlm/.task/.bin/.gguf)")
            }

            if (crashLog != null) {
                Text(
                    text = "마지막 비정상 종료 로그",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    text = crashLog,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                OutlinedButton(
                    onClick = { viewModel.clearCrashLog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    Text("크래시 로그 지우기")
                }
            }
        }
    }
}

private fun DarkThemeMode.label(): String = when (this) {
    DarkThemeMode.FOLLOW_SYSTEM -> "시스템 설정 따르기"
    DarkThemeMode.LIGHT -> "라이트 모드"
    DarkThemeMode.DARK -> "다크 모드"
}

private fun ModelBackend.label(): String = when (this) {
    ModelBackend.CPU -> "CPU (호환성 우선, 약 2.5GB)"
    ModelBackend.GPU -> "GPU (빠른 응답, 약 1.9GB)"
}
