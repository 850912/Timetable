package com.hufeng943.timetable.presentation.ui.screens.more.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

private data class LanguageOption(
    val stableKey: String,
    val tag: String?,
    val labelRes: Int,
)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("system", null, R.string.language_follow_system),
    LanguageOption("zh-CN", "zh-CN", R.string.language_simplified_chinese),
    LanguageOption("en", "en", R.string.language_english),
)

@Composable
fun LanguageSelectPager(config: AppConfig, onLanguageSelect: (String?) -> Unit) {
    val currentTag = config.languageTag
    val initialIndex = remember(currentTag) {
        val index = LANGUAGE_OPTIONS.indexOfFirst { it.tag == currentTag }
        if (index >= 0) index + 1 else 1 // +1 for the header item
    }
    val scrollState = rememberTransformingLazyColumnState(initialAnchorItemIndex = initialIndex)
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item(key = "language_header") {
                ListHeader(
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) { Text(stringResource(R.string.settings_language)) }
            }
            items(LANGUAGE_OPTIONS, key = { it.stableKey }) { option ->
                OneUiCapsuleSurface(
                    title = stringResource(option.labelRes),
                    icon = Icons.Rounded.Language,
                    selected = option.tag == currentTag,
                    onClick = { onLanguageSelect(option.tag) },
                    modifier = Modifier.fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                )
            }
        }
    }
}
