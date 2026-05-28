package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Graphite
import com.example.ui.theme.PremiumGold
import com.example.ui.theme.SubduedGray
import com.example.ui.theme.WarmWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InAppPdfViewer(
    pdfPath: String?,
    pdfUriStr: String? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "DOCUMENT VIEWER",
    onPageChanged: ((currentPage: Int, totalPages: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var pageCount by remember { mutableStateOf(0) }
    var pdfRendererState by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptorState by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Clean up PdfRenderer when disposing
    DisposableEffect(pdfPath, pdfUriStr) {
        isLoading = true
        errorMessage = null
        
        try {
            val pfd = when {
                !pdfPath.isNullOrEmpty() -> {
                    val file = File(pdfPath)
                    if (file.exists()) {
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    } else {
                        null
                    }
                }
                !pdfUriStr.isNullOrEmpty() -> {
                    val uri = Uri.parse(pdfUriStr)
                    context.contentResolver.openFileDescriptor(uri, "r")
                }
                else -> null
            }

            if (pfd != null) {
                fileDescriptorState = pfd
                val renderer = PdfRenderer(pfd)
                pdfRendererState = renderer
                pageCount = renderer.pageCount
                isLoading = false
            } else {
                errorMessage = "Source PDF file clean-path could not be verified offline."
                isLoading = false
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            errorMessage = "Failed to render PDF: ${e.localizedMessage ?: "Resource allocation flaw"}"
            isLoading = false
        }

        onDispose {
            try {
                pdfRendererState?.close()
            } catch (e: Exception) { e.printStackTrace() }
            try {
                fileDescriptorState?.close()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PremiumGold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "DECRYPTING PRESTIGE DECK...",
                    color = PremiumGold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
        return
    }

    if (errorMessage != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "DECODER INTERRUPTED",
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = SubduedGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                ) {
                    Text("BACK TO SAFETY", color = PremiumGold)
                }
            }
        }
        return
    }

    if (pageCount == 0 || pdfRendererState == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No pages detected inside this document.", color = WarmWhite)
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    // Page caching to avoid flickering/re-rendering on page swipe
    val bitmapCache = remember(pdfRendererState) {
        object : java.util.LinkedHashMap<Int, Bitmap>(6, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Bitmap>?): Boolean {
                return size > 6
            }
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage, pageCount) {
        if (pageCount > 0) {
            onPageChanged?.invoke(pagerState.currentPage + 1, pageCount)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) { pageIndex ->
            PdfPageItem(
                renderer = pdfRendererState!!,
                pageIndex = pageIndex,
                bitmapCache = bitmapCache,
                onToggleControls = { controlsVisible = !controlsVisible }
            )
        }

        // Animated overlay for Floating Top Bar (AMOLED viewer chrome)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .shadow(16.dp, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PremiumGold
                        )
                    }

                    Text(
                        text = title.uppercase(),
                        color = PremiumGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                    )

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, PremiumGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / $pageCount",
                            color = PremiumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Animated overlay for Floating Bottom Bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (pagerState.currentPage > 0) {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Prev",
                            tint = if (pagerState.currentPage > 0) PremiumGold else SubduedGray
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "SINGLE-TAP CONTENT TO CHROME SHIFT",
                            fontSize = 9.sp,
                            color = PremiumGold,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PINCH TO ZOOM AND PAN DOCUMENT",
                            fontSize = 8.sp,
                            color = SubduedGray,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (pagerState.currentPage < pageCount - 1) {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < pageCount - 1,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = if (pagerState.currentPage < pageCount - 1) PremiumGold else SubduedGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPageItem(
    renderer: PdfRenderer,
    pageIndex: Int,
    bitmapCache: java.util.LinkedHashMap<Int, Bitmap>,
    onToggleControls: () -> Unit
) {
    val cached = remember(pageIndex) {
        synchronized(bitmapCache) {
            bitmapCache[pageIndex]
        }
    }
    var pageBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(cached) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Render page inside LaunchedEffect to prevent lagging UI thread
    LaunchedEffect(pageIndex) {
        if (pageBitmap == null) {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val page = renderer.openPage(pageIndex)
                    
                    // Scale up target quality for pin-sharp typography in embedded viewing
                    val scaleFactor = 3f
                    val w = (page.width * scaleFactor).toInt()
                    val h = (page.height * scaleFactor).toInt()
                    
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    
                    synchronized(bitmapCache) {
                        bitmapCache[pageIndex] = bmp
                    }
                    bmp
                } catch (e: Throwable) {
                    e.printStackTrace()
                    // Graceful memory fallback: try rendering with 1x scale if 3x triggers OutOfMemoryError
                    try {
                        val page = renderer.openPage(pageIndex)
                        val w = page.width
                        val h = page.height
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        
                        synchronized(bitmapCache) {
                            bitmapCache[pageIndex] = bmp
                        }
                        bmp
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        null
                    }
                }
            }
            pageBitmap = bitmap
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(pageIndex) {
                detectTapGestures(
                    onTap = { onToggleControls() }
                )
            }
            .pointerInput(pageIndex) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    // limit panning bounds when zoomed in
                    if (scale > 1f) {
                        val maxTx = (size.width * (scale - 1f)) / 2f
                        val maxTy = (size.height * (scale - 1f)) / 2f
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxTx, maxTx),
                            y = (offset.y + pan.y).coerceIn(-maxTy, maxTy)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (pageBitmap != null) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .padding(16.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            ) {
                Image(
                    bitmap = pageBitmap!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PremiumGold, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "RECONSTRUCTING GLYPHS...",
                    fontSize = 10.sp,
                    color = SubduedGray,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
