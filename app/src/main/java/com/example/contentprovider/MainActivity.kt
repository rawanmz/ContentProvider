package com.example.contentprovider

import android.Manifest
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.app.ActivityCompat
import coil.compose.AsyncImage
import com.example.contentprovider.ui.theme.ContentProviderTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<ContentProviderViewModel>()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES
            ),
            0
        )
        super.onCreate(savedInstanceState)

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )


        val dateDuration = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -10)//past 2 month
        }.timeInMillis


        val selection = "${MediaStore.Images.Media.DATE_TAKEN} >= ?"
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(dateDuration.toString()),
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )?.use {
            //used to iterate to our data set
            //we have id column and display name column
            val idColumn = it.getColumnIndex(MediaStore.Images.Media._ID)
            val displayNameColumn = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val images = mutableListOf<ContentProviderDataItem>()
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val displayName = it.getString(displayNameColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                images.add(ContentProviderDataItem(id, displayName, uri))
            }
            viewModel.updateImages(images)
        }
        setContent {
            ContentProviderTheme {
                val scale = remember { mutableFloatStateOf(1f) }
                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                scale.floatValue *= zoom
                            }
                        }
                ) {
                    LazyVerticalStaggeredGrid(
                        modifier = Modifier
                            .fillMaxSize(),
                        columns = StaggeredGridCells.Fixed(generateCellsForScale(scale = scale.floatValue))
                    ) {
                        items(viewModel.imagesSate.value) { image ->
                            Column {
                                AsyncImage(
                                    model = image.uri,
                                    contentDescription = "image",
                                    modifier = Modifier
                                )
                                //Text(text = image.name)
                            }
                        }
                    }
                }

//                LazyColumn(Modifier.fillMaxSize()) {
//                    items(viewModel.imagesSate.value) { image ->
//                        Column(Modifier.fillMaxWidth()) {
//                            AsyncImage(model = image.uri, contentDescription = "image")
//                            Text(text = image.name)
//                        }
//                    }
//                }
            }
        }
    }
}

data class ContentProviderDataItem(
    val id: Long,
    val name: String,
    val uri: Uri
)

