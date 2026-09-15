package com.lsing.timego.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun introduction_explains_local_profile_and_can_advance() {
        val viewModel = OnboardingViewModel(ApplicationProvider.getApplicationContext())
        composeRule.setContent {
            MaterialTheme {
                OnboardingScreen(viewModel = viewModel, onFinished = {})
            }
        }

        composeRule.onNodeWithText("Train with better context").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("What are you training for?").assertIsDisplayed()
    }
}
