package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import android.os.ParcelFileDescriptor
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.database.CertificateEntity
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CertificatesVaultScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val certificates by viewModel.certificatesState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourseFilter by remember { mutableStateOf("ALL") }
    var sortByNewest by remember { mutableStateOf(true) }
    
    var certificateToView by remember { mutableStateOf<CertificateEntity?>(null) }
    var pdfPathToViewNatively by remember { mutableStateOf<String?>(null) }

    // Derive course search and filters
    val uniqueCourses = remember(certificates) {
        listOf("ALL") + certificates.map { it.courseName }.distinct()
    }

    val filteredCertificates = remember(certificates, searchQuery, selectedCourseFilter, sortByNewest) {
        var list = certificates.filter {
            (it.courseName.contains(searchQuery, ignoreCase = true) ||
             it.certificateId.contains(searchQuery, ignoreCase = true) ||
             it.userName.contains(searchQuery, ignoreCase = true)) &&
            (selectedCourseFilter == "ALL" || it.courseName == selectedCourseFilter)
        }
        list = if (sortByNewest) {
            list.sortedByDescending { it.completionDate }
        } else {
            list.sortedBy { it.completionDate }
        }
        list
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.background(Graphite, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PremiumGold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AASHIQ+ GOLD VAULT",
                            color = PremiumGold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ACADEMIC TRINKETS & REWARDS",
                            color = WarmWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Luxury Total Info tag
                    Box(
                        modifier = Modifier
                            .background(PremiumGold.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, PremiumGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${filteredCertificates.size} EARNED",
                            color = PremiumGold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant subtle glowing grid behind
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val color = Color(0xFFD4AF37)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = 0.05f), Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 3f),
                                radius = size.width
                            )
                        )
                    }
            )

            if (certificates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Graphite, CircleShape)
                                .border(1.dp, PremiumGold.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PremiumGold.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "PRESTIGE DECK EMPTY",
                            color = PremiumGold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Complete your Masterclasses lessons and unlock dynamic, blockchain-grade cryptographic signatures.",
                            color = SubduedGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Search & Sorting Panel
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search credentials or course name...", color = SubduedGray, fontSize = 12.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PremiumGold) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = PremiumGold)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WarmWhite,
                            unfocusedTextColor = WarmWhite,
                            focusedContainerColor = CharcoalGray,
                            unfocusedContainerColor = CharcoalGray,
                            focusedBorderColor = PremiumGold,
                            unfocusedBorderColor = Graphite
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("certificates_search_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filters & Sorting Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sort Toggle Button
                        Row(
                            modifier = Modifier
                                .clickable { sortByNewest = !sortByNewest }
                                .background(Graphite, RoundedCornerShape(8.dp))
                                .border(0.5.dp, PremiumGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (sortByNewest) Icons.Default.SwapVert else Icons.Default.Sort,
                                contentDescription = null,
                                tint = PremiumGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sortByNewest) "DATE: NEWEST" else "DATE: OLDEST",
                                color = PremiumGold,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Filter Indicator or chips count
                        Text(
                            text = "Showing ${filteredCertificates.size} of ${certificates.size}",
                            fontSize = 11.sp,
                            color = SubduedGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // horizontal filters bar
                    if (uniqueCourses.size > 2) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uniqueCourses) { course ->
                                val isSelected = selectedCourseFilter == course
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) PremiumGold else CharcoalGray,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            0.5.dp,
                                            if (isSelected) PremiumGold else PremiumGold.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedCourseFilter = course }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = course.uppercase(),
                                        color = if (isSelected) Color.Black else WarmWhite,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Grid layout of certificates
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        gridItems(filteredCertificates) { cert ->
                            PremiumCertificateGridCard(
                                certificate = cert,
                                onClick = { certificateToView = cert }
                            )
                        }
                    }
                }
            }
        }
    }

    // Cinematic fullscreen viewer triggering
    if (certificateToView != null) {
        CinematicCertificateDialog(
            certificate = certificateToView!!,
            onDismiss = { certificateToView = null },
            onViewInAppPdf = { pdfPath ->
                pdfPathToViewNatively = pdfPath
            }
        )
    }

    // Fullscreen in-app PDF Viewer overlay
    if (pdfPathToViewNatively != null) {
        Dialog(
            onDismissRequest = { pdfPathToViewNatively = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            InAppPdfViewer(
                pdfPath = pdfPathToViewNatively,
                onClose = { pdfPathToViewNatively = null },
                title = "PRESTIGE VERIFICATION"
            )
        }
    }
}

@Composable
fun PremiumCertificateGridCard(
    certificate: CertificateEntity,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    
    // Rotating seal animation
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                border = BorderStroke(
                    1.dp,
                    Brush.radialGradient(
                        colors = listOf(PremiumGold.copy(alpha = 0.5f), Color.Transparent)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CharcoalGray),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // High prestige certificate seal / header area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color.Black, RoundedCornerShape(10.dp))
                    .border(0.5.dp, PremiumGold.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background thumbnail if generated image exists
                if (!certificate.imagePath.isNullOrEmpty() && File(certificate.imagePath).exists()) {
                    AsyncImage(
                        model = certificate.imagePath,
                        contentDescription = "Thumb",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                            .alpha(0.35f)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = PremiumGold,
                            modifier = Modifier
                                .size(44.dp)
                                .rotate(rotation)
                        )
                        Text(
                            text = "★",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    
                    Text(
                        text = "100% COMPLETE",
                        color = PremiumGold,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Completion Course Title
            Text(
                text = certificate.courseName.uppercase(),
                fontSize = 11.sp,
                color = WarmWhite,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Completion ID
            Text(
                text = "ID: ${certificate.certificateId}",
                fontSize = 8.5.sp,
                color = PremiumGold,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal info divide
            HorizontalDivider(color = Graphite, thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Completion Date Passed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedDate = remember(certificate.completionDate) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    sdf.format(Date(certificate.completionDate))
                }
                
                Text(
                    text = "PASSED $formattedDate",
                    fontSize = 8.sp,
                    color = SubduedGray,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Small badge icon
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = PremiumGold.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun CinematicCertificateDialog(
    certificate: CertificateEntity,
    onDismiss: () -> Unit,
    onViewInAppPdf: (String) -> Unit
) {
    val context = LocalContext.current
    var animateIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateIn = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = animateIn,
                enter = scaleIn(animationSpec = tween(450, easing = FastOutSlowInEasing)) + fadeIn(tween(400)),
                modifier = Modifier.clickable(enabled = false) {} // Prevent click-through dismissal
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalGray),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, PremiumGold),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Text(
                            text = "AASHIQ+ CREDENTIAL DOCKET",
                            color = PremiumGold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // High Fidelity Certificate preview block (Cinematic)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.Black, RoundedCornerShape(12.dp))
                                .border(1.dp, PremiumGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable {
                                    // Direct tap triggers built-in vector PDF Renderer in-app!
                                    if (!certificate.pdfPath.isNullOrEmpty() && File(certificate.pdfPath).exists()) {
                                        onViewInAppPdf(certificate.pdfPath)
                                    } else {
                                        Toast
                                            .makeText(
                                                context,
                                                "Offline PDF format source could not be resolved.",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!certificate.imagePath.isNullOrEmpty() && File(certificate.imagePath).exists()) {
                                AsyncImage(
                                    model = certificate.imagePath,
                                    contentDescription = "Certificate Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        tint = PremiumGold,
                                        modifier = Modifier.size(48.dp),
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("TAP TO COMPOSE PDF PREVIEW", fontSize = 11.sp, color = PremiumGold)
                                }
                            }
                            
                            // Glowing overlay badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, PremiumGold, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.ZoomIn, contentDescription = null, tint = PremiumGold, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("VIEW FULL PDF IN-APP", color = PremiumGold, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Detailed Stats inside Cinematic Sheet
                        Text(
                            text = certificate.courseName.uppercase(),
                            color = WarmWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "OFFLINE ACQUIRED CREDENTIAL",
                            color = PremiumGold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Technical Cryptographic Fields Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            TechnicalStatRow(label = "RECIPIENT", value = certificate.userName)
                            TechnicalStatRow(label = "CREDENTIAL ID", value = certificate.certificateId)
                            TechnicalStatRow(
                                label = "CRYPTOGRAPHIC HASH",
                                value = certificate.hashSignature.take(24) + "..."
                            )
                            val formattedDate = remember(certificate.completionDate) {
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(certificate.completionDate))
                            }
                            TechnicalStatRow(label = "SECURE STAMP", value = formattedDate)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Share / Export Premium Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Share PDF Document easily
                                    shareCertificateFile(context, certificate, isPdf = true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SHARE PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    // Share PNG Image
                                    shareCertificateFile(context, certificate, isPdf = false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Graphite, contentColor = PremiumGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, PremiumGold)
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("EXPORT PNG", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Native Vector PDF Printer Trigger
                        Button(
                            onClick = {
                                printCertificateDocument(context, certificate)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Graphite, contentColor = WarmWhite),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = PremiumGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PRINT PRESTIGE SHEET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = onDismiss) {
                            Text("DISMISS DOCKET", color = SubduedGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TechnicalStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            color = SubduedGray,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontSize = 9.sp,
            color = WarmWhite,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun shareCertificateFile(context: Context, certificate: CertificateEntity, isPdf: Boolean) {
    try {
        val path = if (isPdf) certificate.pdfPath else certificate.imagePath
        if (path.isNullOrEmpty()) {
            Toast.makeText(context, "Asset has not yet been formatted.", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "Prestige file not found locally.", Toast.LENGTH_SHORT).show()
            return
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val mimeType = if (isPdf) "application/pdf" else "image/png"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "CLAIMED CERTIFICATE: ${certificate.courseName}")
            putExtra(Intent.EXTRA_TEXT, "Look at my premium milestone award earned inside Aashiq+: ${certificate.courseName} (Credential ID: ${certificate.certificateId})!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Academic Achievement"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

private fun printCertificateDocument(context: Context, certificate: CertificateEntity) {
    try {
        val path = certificate.pdfPath
        if (path.isNullOrEmpty() || !File(path).exists()) {
            Toast.makeText(context, "Local PDF sheet is not generated yet.", Toast.LENGTH_SHORT).show()
            return
        }

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        val printAdapter = object : android.print.PrintDocumentAdapter() {
            var mPdfDocument: ParcelFileDescriptor? = null

            override fun onLayout(
                oldAttributes: android.print.PrintAttributes?,
                newAttributes: android.print.PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val builder = android.print.PrintDocumentInfo.Builder("Aashiq_Cert_${certificate.certificateId}.pdf")
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                callback?.onLayoutFinished(builder.build(), true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val file = File(path)
                    val input = file.inputStream()
                    val output = FileOutputStream(destination?.fileDescriptor)

                    input.copyTo(output)
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.localizedMessage)
                }
            }
        }

        printManager.print("Aashiq+ Certificate Printing", printAdapter, null)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Printing failure: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
