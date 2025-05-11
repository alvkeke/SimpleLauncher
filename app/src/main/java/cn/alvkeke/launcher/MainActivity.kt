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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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

    Column(modifier = modifier) {
        val itemWidth = remember { mutableStateOf(0.dp) }
        val icon = remember { mutableStateOf<ImageBitmap?>(null) }
        val iconPadding = remember { mutableStateOf(0.dp) }
        val iconWidth = remember { mutableStateOf(0.dp) }

        modifier.onGloballyPositioned{ coordinates ->
            itemWidth.value = coordinates.size.width.toDp()
        }

        val iconModifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .align(Alignment.CenterHorizontally)
            .padding(iconPadding.value)
            .onGloballyPositioned{ coordinates ->
                iconWidth.value = coordinates.size.width.toDp()
                iconPadding.value = iconWidth.value / 4
            }

        if (icon.value == null) {
            Spacer(iconModifier)
            LaunchedEffect(appInfo.packageName) {
                withContext(Dispatchers.IO) {
                    val scaledBitmap = appInfo.loadIcon(context.packageManager)
                        .toBitmap()
                    icon.value = scaledBitmap.asImageBitmap()
                    // do not scale, since the icon size is acceptable
//                     val memSize = icon.value!!.asAndroidBitmap().byteCount
//                     println("icon mem size: ${memSize/1024} KB")
                }
            }
        } else {
            Image(bitmap = icon.value!!, contentDescription = null, iconModifier)
        }
        Spacer(Modifier.width(8.dp))
        Text(text = appInfo.loadLabel(context.packageManager).toString(),
            Modifier.align(Alignment.CenterHorizontally))
    }

}

@Composable
fun AppGrid(list: List<ApplicationInfo>) {
    val itemHeight = remember { mutableStateOf(0.dp) }
    val itemWidth = remember { mutableStateOf(0.dp) }
    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { coordinates ->
            itemHeight.value = coordinates.size.height.toDp() / APP_ROW_PER_PAGE
            itemWidth.value = coordinates.size.width.toDp() / APP_COLUMN_PER_PAGE
        }
        .border(2.dp, Color.Blue)   // FIXME: debug only
    ) {
        list.forEachIndexed { index, app ->
            val row = index / APP_COLUMN_PER_PAGE
            val column = index % APP_COLUMN_PER_PAGE
            AppGridItem(app,
                Modifier
                    .width(itemWidth.value)
                    .height(itemHeight.value)
                    .offset(
                        x = (itemWidth.value * column).coerceAtLeast(0.dp),
                        y = (itemHeight.value * row).coerceAtLeast(0.dp)
                    )
            )
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
    val itemWidth = remember { mutableStateOf(0.dp) }
    Row (modifier = modifier
        .onGloballyPositioned{ coordinates ->
            itemWidth.value = coordinates.size.width.toDp() / APP_PIN_MAX_COUNT
        }
    ){
        for (app in pinedApps) {
            AppGridItem(app,
                Modifier
                    .width(itemWidth.value)
                    .wrapContentHeight()
            )
        }
    }
}

@Composable
fun MainContent(
    pagedApps: List<List<ApplicationInfo>>,
    pinedApps: List<ApplicationInfo>,
    modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val itemHeight = remember { mutableStateOf(0.dp) }
        val itemWidth = remember { mutableStateOf(0.dp) }
        val pagerBottomPadding = remember { mutableStateOf(0.dp) }

        AppPagers(pagedApps,
            Modifier.fillMaxSize()
                .padding(bottom = pagerBottomPadding.value)
                .border(width = 1.dp, color = Color.Green)  // FIXME: debug only
                .onGloballyPositioned { coordinates ->
                    itemHeight.value = coordinates.size.height.toDp() / APP_ROW_PER_PAGE
                    itemWidth.value = coordinates.size.width.toDp() / APP_COLUMN_PER_PAGE
                    pagerBottomPadding.value = itemHeight.value
                }
        )
        Box(modifier = Modifier
            .height(itemHeight.value)
            .align(Alignment.BottomCenter)
        ) {
            PinedBar(pinedApps,
                Modifier
                    .fillMaxSize()
                    .border(width = 1.dp, color = Color.Red) // FIXME: debug only
            )
        }
    }
}


