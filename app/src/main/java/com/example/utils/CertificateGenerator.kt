package com.example.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.data.database.CertificateEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object CertificateGenerator {

    fun generateCertificateFiles(context: Context, certificate: CertificateEntity, profileImageBitmap: Bitmap? = null): Pair<String, String> {
        val width = 1200
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Luxury Black + Gold background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#0F0F11") // Luxury deep space black
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle radiating gold gradient or background accents
        val glowPaint = Paint().apply {
            shader = RadialGradient(
                width / 2f, height / 2f, width * 0.7f,
                Color.parseColor("#1F1C12"), // very soft gold glow
                Color.parseColor("#0F0F11"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)

        // 2. Draw Premium Gold double border lines
        val borderPaint = Paint().apply {
            color = Color.parseColor("#D4AF37") // Premium Gold
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawRect(20f, 20f, (width - 20).toFloat(), (height - 20).toFloat(), borderPaint)

        val innerBorderPaint = Paint().apply {
            color = Color.parseColor("#AA7C11") // Muted Gold
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRect(30f, 30f, (width - 30).toFloat(), (height - 30).toFloat(), innerBorderPaint)

        // Decorative corner ornaments (simple elegant gold angles)
        val cornerPaint = Paint().apply {
            color = Color.parseColor("#E5C158")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        val size = 50f
        // Top Left
        canvas.drawLine(40f, 40f, 40f + size, 40f, cornerPaint)
        canvas.drawLine(40f, 40f, 40f, 40f + size, cornerPaint)
        // Top Right
        canvas.drawLine((width - 40).toFloat(), 40f, (width - 40 - size).toFloat(), 40f, cornerPaint)
        canvas.drawLine((width - 40).toFloat(), 40f, (width - 40).toFloat(), 40f + size, cornerPaint)
        // Bottom Left
        canvas.drawLine(40f, (height - 40).toFloat(), 40f + size, (height - 40).toFloat(), cornerPaint)
        canvas.drawLine(40f, (height - 40).toFloat(), 40f, (height - 40 - size).toFloat(), cornerPaint)
        // Bottom Right
        canvas.drawLine((width - 40).toFloat(), (height - 40).toFloat(), (width - 40 - size).toFloat(), (height - 40).toFloat(), cornerPaint)
        canvas.drawLine((width - 40).toFloat(), (height - 40).toFloat(), (width - 40).toFloat(), (height - 40 - size).toFloat(), cornerPaint)

        // 3. Draw Brand Header
        val brandPaint = Paint().apply {
            color = Color.parseColor("#E5C158")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            letterSpacing = 0.4f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("AASHIQ+ ACADEMY OF EXCELLENCE", width / 2f, 90f, brandPaint)

        val brandSubPaint = Paint().apply {
            color = Color.parseColor("#88888D")
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.2f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("PREMIUM OFFLINE LEARNING GUILD • CERTIFICATE OF COMPLETION", width / 2f, 120f, brandSubPaint)

        // 4. Draw Center Trophy Emblem
        val emblemPaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, 195f, 45f, emblemPaint)
        // Draw star inside
        val starPaint = Paint().apply {
            color = Color.parseColor("#E5C158")
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("★", width / 2f, 210f, starPaint)

        // 5. User Profile Circle Image
        val avatarCenterX = width / 2f
        val avatarCenterY = 310f
        val avatarRadius = 40f
        val avatarBorderPaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        
        if (profileImageBitmap != null) {
            val destRect = RectF(
                avatarCenterX - avatarRadius, avatarCenterY - avatarRadius,
                avatarCenterX + avatarRadius, avatarCenterY + avatarRadius
            )
            val path = Path().apply {
                addCircle(avatarCenterX, avatarCenterY, avatarRadius, Path.Direction.CCW)
            }
            canvas.save()
            try {
                canvas.clipPath(path)
                val srcRect = Rect(0, 0, profileImageBitmap.width, profileImageBitmap.height)
                canvas.drawBitmap(profileImageBitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
            } catch (e: Exception) {
                // draw fallback
                val bgAvatar = Paint().apply {
                    color = Color.parseColor("#221D12")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, bgAvatar)
            }
            canvas.restore()
            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBorderPaint)
        } else {
            // Draw default luxury profile monogram
            val bgAvatar = Paint().apply {
                color = Color.parseColor("#221D12")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, bgAvatar)
            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBorderPaint)
            // Monogram letter
            val letterPaint = Paint().apply {
                color = Color.parseColor("#E5C158")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val initials = if (certificate.userName.isNotEmpty()) certificate.userName.take(1).uppercase() else "A"
            canvas.drawText(initials, avatarCenterX, avatarCenterY + 10f, letterPaint)
        }

        // 6. Recipient Announcement
        val isCertifiedPaint = Paint().apply {
            color = Color.parseColor("#88888D")
            textSize = 15f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("This premium academic credential is is honourably presented to:", width / 2f, 395f, isCertifiedPaint)

        // Recipient Name
        val namePaint = Paint().apply {
            color = Color.parseColor("#E5C158") // Glowing Gold
            textSize = 38f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(certificate.userName, width / 2f, 450f, namePaint)

        // Divider
        val divPaint = Paint().apply {
            color = Color.parseColor("#443A1F")
            strokeWidth = 1.5f
        }
        canvas.drawLine(width / 2f - 180f, 470f, width / 2f + 180f, 470f, divPaint)

        // Accomplishment Text
        val courseAnnouncePaint = Paint().apply {
            color = Color.parseColor("#88888D")
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("for successfully achieving mastery and 100% full course completion of:", width / 2f, 505f, courseAnnouncePaint)

        // Course Title
        val courseTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(certificate.courseName, width / 2f, 555f, courseTitlePaint)

        // Completion percentage and Date Details
        val dateText = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(certificate.completionDate))
        val detailsPaint = Paint().apply {
            color = Color.parseColor("#C5A650")
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("COMPLETED ON: $dateText  •  CREDENTIAL STATUS: HONORS (100% VALIDATED)", width / 2f, 605f, detailsPaint)

        // 7. Footer & Signatures
        val footerLabelPaint = Paint().apply {
            color = Color.parseColor("#5A5A62")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        
        // Left side: Signature
        val sigPaint = Paint().apply {
            color = Color.parseColor("#E5C158")
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("Aashiq Ali", 200f, 680f, sigPaint)
        canvas.drawLine(120f, 695f, 280f, 695f, divPaint)
        canvas.drawText("FOUNDER & CHIEF MASTER", 200f, 715f, footerLabelPaint)

        // Center: Certificate Unique ID
        val hashPaint = Paint().apply {
            color = Color.parseColor("#4B4B4B")
            textSize = 10f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CREDENTIAL ID: ${certificate.certificateId}", width / 2f, 675f, hashPaint)
        canvas.drawText("BLOCKCHAIN VERIFICATION SIGNATURE: ${certificate.hashSignature}", width / 2f, 695f, hashPaint)

        // Right side: Authenticity Seal Label
        canvas.drawText("AASHIQ+ PREMIUM", 1000f, 680f, brandSubPaint.apply { color = Color.parseColor("#E5C158"); textSize = 16f })
        canvas.drawLine(920f, 695f, 1080f, 695f, divPaint)
        canvas.drawText("OFFLINE SECURITY SEAL", 1000f, 715f, footerLabelPaint)

        // Save Bitmap as High Quality PNG
        val certificateDir = File(context.filesDir, "certificates")
        if (!certificateDir.exists()) {
            certificateDir.mkdirs()
        }

        val pngFile = File(certificateDir, "AASHIQ_CERT_${certificate.certificateId}.png")
        FileOutputStream(pngFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Save PDF using Android PdfDocument
        val pdfFile = File(certificateDir, "AASHIQ_CERT_${certificate.certificateId}.pdf")
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        
        // Draw the bitmap on the PDF Canvas
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)
        
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return Pair(pngFile.absolutePath, pdfFile.absolutePath)
    }
}
