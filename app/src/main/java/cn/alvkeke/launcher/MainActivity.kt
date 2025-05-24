package cn.alvkeke.launcher

import android.app.WallpaperManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import cn.alvkeke.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


const val APP_PIN_MAX_COUNT = 4
const val APP_COLUMN_PER_PAGE = 4
const val APP_ROW_PER_PAGE = 6
const val APP_COUNT_PER_PAGE = APP_COLUMN_PER_PAGE * APP_ROW_PER_PAGE

const val PADDING_APP_CONTAINER = 8 // padding for each app item

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

        val manager = WallpaperManager.getInstance(this)
        val wallpaperColors = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        val primaryColor = wallpaperColors?.primaryColor
        val composeColor = if (primaryColor != null) {
            Color(primaryColor.toArgb())
        } else {
            Color.Black // treat as black background
        }

        val darkness = composeColor.getDarkness()
        val lightTheme = darkness < 0.5f
        println("==================== darkness: $darkness, lightTheme: $lightTheme")
        WindowCompat
            .getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = lightTheme
        val textColor: Color = if (lightTheme)
            Color.Black
        else
            Color.White

        setContent {
            LauncherTheme {
                MainContent(
                    pagedApps = pagedApps,
                    pinedApps = pinedApps,
                    textColor = textColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                )
            }
        }
    }
}

fun Color.getDarkness(): Float {
    val darkness: Float = 1 - (0.299f * this.red + 0.587f * this.green + 0.114f * this.blue) / 255
    return darkness
}

@Composable
fun AppItem(
    appInfo: ApplicationInfo,
    textColor: Color,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    // add Image item for App icon
    val context = LocalContext.current

    Column(modifier = modifier
        .width(width = width)
        .height(height = height)
    ) {
        val itemWidth = width
        val icon = remember { mutableStateOf<ImageBitmap?>(null) }
        val iconWidth = itemWidth * 2 / 3

        val iconModifier = Modifier
            .width(iconWidth)
            .aspectRatio(1f)
            .align(Alignment.CenterHorizontally)

        if (icon.value == null) {
            Spacer(iconModifier)
            LaunchedEffect(appInfo.packageName) {
                withContext(Dispatchers.IO) {
                    val scaledBitmap = appInfo.loadIcon(context.packageManager)
                        .toBitmap()
                    icon.value = scaledBitmap.asImageBitmap()
                    // do not scale, since the icon size is acceptable
                    // val memSize = icon.value!!.asAndroidBitmap().byteCount
                    // println("icon mem size: ${memSize/1024} KB")
                }
            }
        } else {
            Image(bitmap = icon.value!!, contentDescription = null, iconModifier)
        }
        Spacer(Modifier.width(8.dp))
        Text(text = appInfo.loadLabel(context.packageManager).toString(),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(),
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }

}

@Composable
fun AppGrid(list: List<ApplicationInfo>, textColor: Color) {
    BoxWithConstraints (modifier = Modifier
        .fillMaxSize()
        .padding(PADDING_APP_CONTAINER.dp)
    ) {
        val scope = this
        val itemHeight = scope.maxHeight / APP_ROW_PER_PAGE
        val itemWidth = scope.maxWidth / APP_COLUMN_PER_PAGE

        list.forEachIndexed { index, app ->
            val row = index / APP_COLUMN_PER_PAGE
            val column = index % APP_COLUMN_PER_PAGE
            AppItem(app,
                textColor,
                itemWidth,
                itemHeight,
                Modifier
                    .offset(
                        x = (itemWidth * column).coerceAtLeast(0.dp),
                        y = (itemHeight * row).coerceAtLeast(0.dp)
                    )
            )
        }
    }
}

@Composable
fun AppPageContent(
    list: List<ApplicationInfo>,
    textColor: Color,
) {
    AppGrid(list, textColor)
}

@Composable
fun AppPagers(list: List<List<ApplicationInfo>>, textColor: Color, modifier: Modifier = Modifier) {
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
        AppPageContent (list[page], textColor)
    }
}

@Composable
fun PinedBar(pinedApps: List<ApplicationInfo>, textColor: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints (modifier = modifier.padding(PADDING_APP_CONTAINER.dp)){
        val scope = this
        val itemWidth = scope.maxWidth / APP_PIN_MAX_COUNT

        for (app in pinedApps) {
            AppItem(app,
                textColor,
                itemWidth,
                scope.maxHeight,
                Modifier
                    .offset(x = (itemWidth * pinedApps.indexOf(app)).coerceAtLeast(0.dp) )
            )
        }
    }
}

@Composable
fun MainContent(
    pagedApps: List<List<ApplicationInfo>>,
    pinedApps: List<ApplicationInfo>,
    textColor: Color,
    modifier: Modifier = Modifier) {
    BoxWithConstraints (modifier = modifier) {
        val scope = this
        val itemHeight = scope.maxHeight / (APP_ROW_PER_PAGE + 1)
        // val itemWidth = scope.maxWidth / APP_COLUMN_PER_PAGE
        val pagerBottomPadding = itemHeight

        AppPagers(pagedApps,
            textColor,
            Modifier.fillMaxSize()
                .padding(bottom = pagerBottomPadding)
        )
        PinedBar(pinedApps,
            textColor,
            Modifier
                .height(itemHeight)
                .fillMaxWidth()
                .align(Alignment.BottomStart)
        )
    }
}


