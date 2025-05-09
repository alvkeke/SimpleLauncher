package cn.alvkeke.launcher

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import cn.alvkeke.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


const val APP_PIN_MAX_COUNT = 4
const val APP_COLUMN_PER_PAGE = 4
const val APP_ROW_PER_PAGE = 6
const val APP_COUNT_PER_PAGE = APP_COLUMN_PER_PAGE * APP_ROW_PER_PAGE

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
            }
        println("Apps count: ${allApps.size}")

        // TODO: fix
        val pinedApps: List<ApplicationInfo> = allApps.subList(0, 4)
        allApps = allApps.subList(APP_PIN_MAX_COUNT, allApps.size)

        // move apps into a new list
        val pagedApps = ArrayList<List<ApplicationInfo>>()
        for (i in allApps.indices step APP_COUNT_PER_PAGE) {
            val end = if (i + APP_COUNT_PER_PAGE < allApps.size)
                    i + APP_COUNT_PER_PAGE else allApps.size
            pagedApps.add(allApps.subList(i, end))
        }

        println("Page count: ${pagedApps.size}")
        for (i in 0 until pagedApps.size) {
            val page = pagedApps[i]
            println("Page-$i, app count: ${page.size}")
        }

        setContent {
            LauncherTheme {
                MainContent(
                    pagedApps = pagedApps,
                    pinedApps = pinedApps,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                )
            }
        }
    }
}

fun Int.toDp(): Dp = (this / Resources.getSystem().displayMetrics.density).dp

@Composable
fun AppGridItem(
    appInfo: ApplicationInfo,
    modifier: Modifier = Modifier
) {
    // add Image item for App icon
    val context = LocalContext.current
    val icon = remember { mutableStateOf<ImageBitmap?>(null) }
    val iconSize = 48.dp

    if (icon.value == null) {
        LaunchedEffect(appInfo.packageName) {
            withContext(Dispatchers.IO) {
                val scaleFactor = 3
                val scaledBitmap = appInfo.loadIcon(context.packageManager)
                    .toBitmap()
                    .scale(
                        iconSize.value.toInt() * scaleFactor,
                        iconSize.value.toInt() * scaleFactor,
                        true
                    )
                icon.value = scaledBitmap.asImageBitmap()
            }
        }
    }

    Column(modifier = modifier.padding(8.dp).height(100.dp)) {
        val imageModifier = Modifier.width(iconSize)
            .height(iconSize)
            .align(Alignment.CenterHorizontally)
        if (icon.value == null) {
            Spacer(imageModifier)
        } else {
            Image(bitmap = icon.value!!, contentDescription = null, imageModifier)
        }
        Spacer(Modifier.width(8.dp))
        Text(text = appInfo.loadLabel(context.packageManager).toString(),
            Modifier.align(Alignment.CenterHorizontally))
    }

}

@Composable
fun AppGrid(list: List<ApplicationInfo>) {
    LazyVerticalGrid (
        columns = GridCells.Fixed(4),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(list.size) { idx ->
            AppGridItem(list[idx])
        }
    }
}

@Composable
fun AppPageContent(
    list: List<ApplicationInfo>
) {
    AppGrid(list)
}

@Composable
fun AppPagers(list: List<List<ApplicationInfo>>, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState {
        list.size
    }
    HorizontalPager(state = pagerState,
        modifier = modifier,
        // make all pages be loaded on initialization
        // think this should not be the best practice, but it works
        // TODO: find a better way to load pages without lag
        beyondViewportPageCount = list.size
    ) { page ->
        AppPageContent (list[page])
    }
}

@Composable
fun PinedBar(pinedApps: List<ApplicationInfo>, modifier: Modifier = Modifier) {
    Row (modifier = modifier){
        for (app in pinedApps) {
            AppGridItem(app)
        }
    }
}

@Composable
fun MainContent(
    pagedApps: List<List<ApplicationInfo>>,
    pinedApps: List<ApplicationInfo>,
    modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val bottomBarHeight = remember { mutableStateOf(0.dp) }

        AppPagers(pagedApps,
            Modifier.fillMaxSize()
                .padding(bottom = bottomBarHeight.value)
                .border(width = 2.dp, color = Color.Green)  // FIXME: debug only
        )
        Box(modifier = Modifier
            .align(Alignment.BottomCenter)
            .onGloballyPositioned { coordinates ->
                bottomBarHeight.value = coordinates.size.height.toDp()
            }
        ) {
            PinedBar(pinedApps)
        }
    }
}


