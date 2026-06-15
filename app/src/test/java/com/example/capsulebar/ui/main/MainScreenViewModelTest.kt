package com.example.capsulebar.ui.main

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainScreenViewModelTest {

    private lateinit var context: Context
    private lateinit var viewModel: MainScreenViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = MainScreenViewModel(context)
    }

    @Test
    fun initialValues_areCorrect() = runTest {
        // Default values from CapsuleSettings
        assertEquals(0, viewModel.xOffset.value)
        assertEquals(40, viewModel.yOffset.value)
        assertEquals(110, viewModel.widthDp.value)
        assertEquals(36, viewModel.heightDp.value)
        assertEquals(18, viewModel.cornerRadiusDp.value)
    }

    @Test
    fun updateOffsets_updatesStateFlows() = runTest {
        viewModel.updateXOffset(10)
        viewModel.updateYOffset(50)
        
        assertEquals(10, viewModel.xOffset.value)
        assertEquals(50, viewModel.yOffset.value)
    }

    @Test
    fun updateDimensions_updatesStateFlows() = runTest {
        viewModel.updateWidthDp(150)
        viewModel.updateHeightDp(45)
        viewModel.updateCornerRadiusDp(20)
        
        assertEquals(150, viewModel.widthDp.value)
        assertEquals(45, viewModel.heightDp.value)
        assertEquals(20, viewModel.cornerRadiusDp.value)
    }
}
