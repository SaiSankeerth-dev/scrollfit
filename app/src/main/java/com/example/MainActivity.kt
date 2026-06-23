package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ScrollFitApp
import com.example.ui.ScrollFitViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    com.example.tracking.TrackingController.requestRuntimePermissions(this)
    setContent {
      MyApplicationTheme {
        val viewModel: ScrollFitViewModel = viewModel()
        ScrollFitApp(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    com.example.tracking.TrackingController