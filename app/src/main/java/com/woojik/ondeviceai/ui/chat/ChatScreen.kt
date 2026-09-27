package com.woojik.ondeviceai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woojik.ondeviceai.OnDeviceAiApplication
import com.woojik.ondeviceai.R
import com.woojik.ondeviceai.data.model.ChatMessage

/**
 * 메인 채팅 화면.
 * 구조: 캐릭터 영역 / 메시지 목록 / 입력 영역 / 설정 진입점.
 * PRD-02: 생성 중 표시, 스트리밍 응답, 취소, 오류 표시가 추가되어도 구조는 유지된다.
 */
@Composable
fun ChatScreen(
    characterName: String,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val locator = (context.applicationContext as OnDeviceAiApplication).locator
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.factory(locator))
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()
    val listState = rememberLazyListState()

    val showStreaming = isGenerating || streamingText.isNotEmpty()
    val lastItemIndex = messages.size + if (showStreaming) 1 else 0
    LaunchedEffect(lastItemIndex) {
        if (lastItemIndex > 0) listState.animateScrollToItem(lastItemIndex - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        CharacterArea(
            name = characterName,
            isGenerating = isGenerating,
            onOpenSettings = onOpenSettings,
        )
        MessageList(
            messages = messages,
            streamingText = streamingText,
            isGenerating = isGenerating,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        if (error != null) {
            ErrorBar(
                message = error!!.userMessage,
                onDismiss = viewModel::dismissError,
            )
        }
        InputArea(
            text = inputText,
            isGenerating = isGenerating,
            onTextChange = viewModel::onInputChange,
            onSend = viewModel::send,
            onCancel = viewModel::cancelGeneration,
        )
    }
}

/** 상단 캐릭터 표시 영역 + 설정 진입점 */
@Composable
private fun CharacterArea(name: String, isGenerating: Boolean, onOpenSettings: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = dimensionResource(R.dimen.chat_padding), vertical = 8.dp)
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = if (isGenerating) "응답을 만들고 있어요…" else "대화를 시작해 보세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "설정",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** 대화 목록 + 스트리밍 중인 임시 응답 */
@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    streamingText: String,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
) {
    val showStreaming = isGenerating || streamingText.isNotEmpty()
    if (messages.isEmpty() && !showStreaming) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "메시지를 입력해 보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        state = rememberLazyListState(),
        modifier = modifier
            .padding(horizontal = dimensionResource(R.dimen.chat_padding)),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(message)
        }
        if (showStreaming) {
            item(key = "streaming") {
                StreamingBubble(
                    text = streamingText,
                    isGenerating = isGenerating,
                )
            }
        }
    }
}

/** 개별 메시지 버블 */
@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatMessage.Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

/** 생성 중(스트리밍) 응답 버블 */
@Composable
private fun StreamingBubble(text: String, isGenerating: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isGenerating && text.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(14.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    text = if (text.isEmpty()) "…" else text + if (isGenerating) "…" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 복구 가능한 오류 안내 */
@Composable
private fun ErrorBar(message: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = dimensionResource(R.dimen.chat_padding), vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    }
}

/** 하단 입력 영역: 텍스트 입력창 + 전송/취소 버튼 */
@Composable
private fun InputArea(
    text: String,
    isGenerating: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.chat_padding)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("메시지 입력") },
                maxLines = 4,
                enabled = !isGenerating,
            )
            if (isGenerating) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "생성 취소",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "전송",
                        tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
