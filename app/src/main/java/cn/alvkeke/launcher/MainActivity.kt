package cn.alvkeke.launcher

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import cn.alvkeke.launcher.ui.theme.LauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val testList: ArrayList<String> = ArrayList()
        for (i in 0..10) {
            testList.add(i.toString())
        }

        var appList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
            }

        println("Apps count: ${appList.size}")

        for (i in appList) {
            if (i == null) continue
        }

        setContent {
            LauncherTheme {
                    AppGrid(
                        list = appList
                    )
            }
        }
    }
}

@Composable
fun AppGridItem(
    appInfo: ApplicationInfo,
    modifier: Modifier = Modifier
) {
    // add Image item for App icon
    val context = LocalContext.current
    val icon = remember { mutableStateOf<ImageBitmap?>(null) }
    val iconSize = 48.dp

    LaunchedEffect(appInfo.packageName) {
        icon.value = appInfo.loadIcon(context.packageManager).toBitmap().asImageBitmap()
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
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(4),
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(list.size) { idx ->
            AppGridItem(list[idx])
        }
    }
}
