package com.woojik.ondeviceai.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

/** 설정 화면: 테마 모드 선택, 캐릭터 이름 변경 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locator = (context.applicationContext as OnDeviceAiApplication).locator
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(locator))
    val settings by viewModel.settings.collectAsState()

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
        }
    }
}

private fun DarkThemeMode.label(): String = when (this) {
    DarkThemeMode.FOLLOW_SYSTEM -> "시스템 설정 따르기"
    DarkThemeMode.LIGHT -> "라이트 모드"
    DarkThemeMode.DARK -> "다크 모드"
}
