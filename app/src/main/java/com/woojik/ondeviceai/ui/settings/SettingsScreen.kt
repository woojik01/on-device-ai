package com.woojik.ondeviceai.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woojik.ondeviceai.OnDeviceAiApplication
import com.woojik.ondeviceai.data.model.DarkThemeMode

/** 설정 화면: 테마 모드 선택, 캐릭터 이름 변경, 로컬 모델 다운로드/가져오기 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locator = (context.applicationContext as OnDeviceAiApplication).locator
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(locator))
    val settings by viewModel.settings.collectAsState()
    val modelState by viewModel.modelState.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importModel(uri)
    }

    var nameInput by remember { mutableStateOf("") }
    LaunchedEffect(settings.characterName) {
        nameInput = settings.characterName
    }

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
                .padding(16.dp),
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
                            text = "모델 다운로드 중이에요. (약 1.3GB, Wi-Fi 권장)",
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
                Text("모델 자동 다운로드 (Gemma 2B, 약 1.3GB)")
            }
            Button(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                enabled = modelState !is SettingsViewModel.ModelState.Downloading &&
                    modelState !is SettingsViewModel.ModelState.Importing,
            ) {
                Text("파일로 직접 가져오기 (.task/.bin/.gguf)")
            }
        }
    }
}

private fun DarkThemeMode.label(): String = when (this) {
    DarkThemeMode.FOLLOW_SYSTEM -> "시스템 설정 따르기"
    DarkThemeMode.LIGHT -> "라이트 모드"
    DarkThemeMode.DARK -> "다크 모드"
}
