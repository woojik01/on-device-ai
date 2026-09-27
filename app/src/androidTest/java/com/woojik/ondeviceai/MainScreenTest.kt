package com.woojik.ondeviceai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PRD-01 시나리오 UI 테스트:
 * 앱 실행 → 채팅 화면 표시 → 메시지 입력 → 전송 → 설정 화면 이동.
 * 실기기/에뮬레이터에서 실행된다.
 */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun chatScreenShowsInputField() {
        composeRule.onNodeWithText("메시지 입력").assertIsDisplayed()
    }

    @Test
    fun sendMessageAppearsInMessageList() {
        composeRule.onNodeWithText("메시지 입력").performTextInput("안녕하세요")
        composeRule.onNodeWithContentDescription("전송").performClick()
        composeRule.onNodeWithText("안녕하세요").assertIsDisplayed()
    }

    @Test
    fun settingsNavigationWorks() {
        composeRule.onNodeWithContentDescription("설정").performClick()
        composeRule.onNodeWithText("테마").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("뒤로").performClick()
        composeRule.onNodeWithText("메시지 입력").assertIsDisplayed()
    }
}
