package com.hufeng943.timetable.presentation.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults

@Composable
fun HomeScreen() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    var isDatePickerOpen by remember { mutableStateOf(false) }

    // Official Wear Material 3 pager stack: the scaffold owns indicator/time transitions and
    // AnimatedPage supplies the platform scaling/scrim treatment instead of a custom animation.
    HorizontalPagerScaffold(
        pagerState = pagerState,
        modifier = Modifier.fillMaxSize(),
        pageIndicator = {
            if (!isDatePickerOpen) {
                androidx.wear.compose.material3.HorizontalPageIndicator(pagerState = pagerState)
            }
        },
    ) {
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            userScrollEnabled = !isDatePickerOpen,
            flingBehavior = PagerScaffoldDefaults.snapWithSpringFlingBehavior(pagerState),
        ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                when (page) {
                    0 -> TimetablePager(onOpenStateChanged = { isDatePickerOpen = it })
                    else -> MorePager()
                }
            }
        }
    }
}
