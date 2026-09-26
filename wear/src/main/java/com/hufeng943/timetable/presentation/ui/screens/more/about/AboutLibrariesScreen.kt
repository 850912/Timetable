package com.hufeng943.timetable.presentation.ui.screens.more.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hufeng943.timetable.R
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.wear.LibrariesContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AboutLibrariesScreen() {
    val context = LocalContext.current

    val libraries by produceLibraries {
        withContext(Dispatchers.IO) {
            context.resources
                .openRawResource(R.raw.aboutlibraries)
                .bufferedReader()
                .use { it.readText() }
        }
    }

    LibrariesContainer(
        libraries = libraries,
        modifier = Modifier.fillMaxSize()
    )
}
