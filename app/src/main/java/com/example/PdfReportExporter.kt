package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfReportExporter {

    fun generateAndSharePdf(
        context: Context,
        result: CalculationResult,
        lang: LanguageCode,
        loanType: String
    ): Boolean {
        if (!result.isValid) return false

        try {
            val pdfDocument = PdfDocument()
            
            // Standard A4 dimensions
            val pageWidth = 595
            val pageHeight = 842
            
            // Paints
            val titlePaint = Paint().apply {
                color = Color.parseColor("#111827")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val h2Paint = Paint().apply {
                color = Color.parseColor("#1F2937")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val valBigPaint = Paint().apply {
                color = Color.parseColor("#10B981") // Emerald Green
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.parseColor("#374151")
                textSize = 9f
                isAntiAlias = true
            }

            val bodyBoldPaint = Paint().apply {
                color = Color.parseColor("#1F2937")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bgPaint = Paint().apply {
                color = Color.parseColor("#1C232B") // Slate Dark Header
                style = Paint.Style.FILL
            }

            val lightBgPaint = Paint().apply {
                color = Color.parseColor("#F3F4F6")
                style = Paint.Style.FILL
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }

            val sym = lang.currencySymbol

            // PAGE 1: Overview & Performance Breakdown
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Brand Header Accent Bar
            val accentPaint = Paint().apply {
                color = Color.parseColor("#10B981")
                style = Paint.Style.FILL
            }
            canvas.drawRect(40f, 40f, 555f, 44f, accentPaint)

            // Header Title
            canvas.drawText(
                Translations.get(TranslationKey.APP_TITLE, lang),
                40f,
                75f,
                titlePaint
            )

            // Subtitle metadata
            val subText = "Report Category: $loanType | Generated on: 2026-05-30"
            val infoPaint = Paint().apply {
                color = Color.parseColor("#6B7280")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }
            canvas.drawText(subText, 40f, 95f, infoPaint)

            // Summary Grid Divider
            canvas.drawLine(40f, 110f, 555f, 110f, linePaint)

            // Row 1 Metadata Cards
            val cardY = 125f
            val cardW = 160f
            val cardH = 50f
            
            // Principal Card
            drawMetadataCard(canvas, 40f, cardY, cardW, cardH, lightBgPaint, linePaint, 
                Translations.get(TranslationKey.LOAN_AMOUNT, lang), 
                formatCurrency(result.principalLoanAmount, sym), bodyPaint, valBigPaint)
            
            // Total Monthly Card
            drawMetadataCard(canvas, 215f, cardY, cardW, cardH, lightBgPaint, linePaint, 
                Translations.get(TranslationKey.MONTHLY_PAYMENT, lang), 
                formatCurrency(result.totalMonthlyPaymentWithFees, sym), bodyPaint, valBigPaint)
            
            // Total Interest Card
            drawMetadataCard(canvas, 390f, cardY, cardW, cardH, lightBgPaint, linePaint, 
                Translations.get(TranslationKey.TOTAL_INTEREST, lang), 
                formatCurrency(result.totalInterestPaid, sym), bodyPaint, valBigPaint)

            // Row 2 Metadata Cards
            val cardY2 = 185f
            
            // Time Saved Early
            val yearsSaved = result.savingYearsEarly
            val timeSavedText = if (yearsSaved > 0.0) "${yearsSaved.format(1)} y early" else "On schedule"
            drawMetadataCard(canvas, 40f, cardY2, cardW, cardH, lightBgPaint, linePaint, 
                "Time Saved Early", 
                timeSavedText, bodyPaint, valBigPaint)
            
            // Repayment period
            val periodVal = "${result.actualRepaymentMonths} m / ${(result.actualRepaymentMonths / 12.0).format(1)} y"
            drawMetadataCard(canvas, 215f, cardY2, cardW, cardH, lightBgPaint, linePaint, 
                "Repayment Period", 
                periodVal, bodyPaint, valBigPaint)

            // Total Cost
            drawMetadataCard(canvas, 390f, cardY2, cardW, cardH, lightBgPaint, linePaint, 
                Translations.get(TranslationKey.TOTAL_LOAN_COST, lang), 
                formatCurrency(result.totalPaidAmount, sym), bodyPaint, valBigPaint)

            // Section: Table Header for amortization schedule
            canvas.drawText(
                Translations.get(TranslationKey.SCHEDULE_TAB, lang) + " (" + Translations.get(TranslationKey.YEAR_LABEL, lang) + "ly)",
                40f,
                265f,
                h2Paint
            )

            // Draw Table Headers
            val tableY = 285f
            canvas.drawRect(40f, tableY, 555f, tableY + 22f, bgPaint)
            
            val colsX = floatArrayOf(45f, 100f, 180f, 270f, 360f, 455f)
            val headerKeys = arrayOf(
                Translations.get(TranslationKey.YEAR, lang),
                "Total Paid",
                Translations.get(TranslationKey.PRINCIPAL_AND_INTEREST, lang).take(12) + "..",
                Translations.get(TranslationKey.TOTAL_INTEREST, lang).take(12) + "..",
                "Extra Paid",
                "Ending Balance"
            )

            for (i in headerKeys.indices) {
                canvas.drawText(headerKeys[i], colsX[i], tableY + 14f, headerTextPaint)
            }

            var currY = tableY + 22f
            var itemCounter = 0
            var pageNumber = 1

            // Write Yearly Items
            for (item in result.yearlySchedule) {
                // If we run out of vertical space on current page, finish it and start a new one!
                if (currY + 20f > pageHeight - 60f) {
                    // Draw Page footer on old page
                    drawFooter(canvas, pageNumber, sym, pageWidth, pageHeight, bodyPaint, linePaint)
                    pdfDocument.finishPage(page)
                    
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    
                    // Draw Table Header Again
                    canvas.drawRect(40f, 40f, 555f, 62f, bgPaint)
                    for (i in headerKeys.indices) {
                        canvas.drawText(headerKeys[i], colsX[i], 54f, headerTextPaint)
                    }
                    currY = 62f
                }

                // Row content bg striping
                if (itemCounter % 2 == 1) {
                    canvas.drawRect(40f, currY, 555f, currY + 18f, lightBgPaint)
                }
                
                canvas.drawText(item.yearNumber.toString(), colsX[0], currY + 12f, bodyBoldPaint)
                canvas.drawText(formatCurrency(item.paymentAmount, sym), colsX[1], currY + 12f, bodyPaint)
                canvas.drawText(formatCurrency(item.principalPaid, sym), colsX[2], currY + 12f, bodyPaint)
                canvas.drawText(formatCurrency(item.interestPaid, sym), colsX[3], currY + 12f, bodyPaint)
                canvas.drawText(formatCurrency(item.extraPayment, sym), colsX[4], currY + 12f, bodyPaint)
                canvas.drawText(formatCurrency(item.endingBalance, sym), colsX[5], currY + 12f, bodyBoldPaint)

                // Dividers
                canvas.drawLine(40f, currY + 18f, 555f, currY + 18f, linePaint)
                
                currY += 18f
                itemCounter++
            }

            // Draw Final Page Footer
            drawFooter(canvas, pageNumber, sym, pageWidth, pageHeight, bodyPaint, linePaint)
            pdfDocument.finishPage(page)

            // Save PDF to cache dir & share it
            val cacheFile = File(context.cacheDir, "loan_amortization_report.pdf")
            val outputStream = FileOutputStream(cacheFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()

            // Trigger File Sharing via Intent Provider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Mortgage & Loan Amortization Schedule")
                putExtra(Intent.EXTRA_TEXT, "Here is the PDF Amortization report of your calculated loan.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share Report PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            return true

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun generateAndShareDebtPlanPdf(
        context: Context,
        debts: List<Debt>,
        budget: Double,
        strategy: String,
        result: DebtPlannerResult,
        lang: LanguageCode
    ): Boolean {
        if (!result.isValid) return false
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            // Paints
            val titlePaint = Paint().apply {
                color = Color.parseColor("#111827")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val h2Paint = Paint().apply {
                color = Color.parseColor("#1F2937")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val valBigPaint = Paint().apply {
                color = Color.parseColor("#F43F5E") // Rose Pink accent
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.parseColor("#374151")
                textSize = 8.5f
                isAntiAlias = true
            }

            val bodyBoldPaint = Paint().apply {
                color = Color.parseColor("#1F2937")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bgPaint = Paint().apply {
                color = Color.parseColor("#0F172A") // Elegant dark header bg
                style = Paint.Style.FILL
            }

            val lightBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 0.7f
            }

            val sym = lang.currencySymbol

            // PAGE 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Brand Header Accent Bar
            val accentPaint = Paint().apply {
                color = Color.parseColor("#F43F5E")
                style = Paint.Style.FILL
            }
            canvas.drawRect(40f, 40f, 555f, 44f, accentPaint)

            canvas.drawText(
                Translations.get(TranslationKey.DEBT_PLANNER_TAB, lang),
                40f,
                70f,
                titlePaint
            )

            val detailsText = "Strategy: $strategy | Monthly Budget: ${formatCurrency(budget, sym)} | Total Registered Debts: ${debts.size}"
            val infoPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }
            canvas.drawText(detailsText, 40f, 88f, infoPaint)

            canvas.drawLine(40f, 102f, 555f, 102f, linePaint)

            // Performance Cards
            val cardY = 114f
            val cardW = 160f
            val cardH = 46f

            drawMetadataCard(canvas, 40f, cardY, cardW, cardH, lightBgPaint, linePaint,
                "Total Starting Debt",
                formatCurrency(result.totalStartingDebt, sym), bodyPaint, valBigPaint)

            val freedTime = "${result.debtFreeMonths} months"
            drawMetadataCard(canvas, 215f, cardY, cardW, cardH, lightBgPaint, linePaint,
                "Projected Payoff Time",
                freedTime, bodyPaint, valBigPaint)

            drawMetadataCard(canvas, 390f, cardY, cardW, cardH, lightBgPaint, linePaint,
                "Total Interest Cost",
                formatCurrency(result.totalInterestPaid, sym), bodyPaint, valBigPaint)

            // Row 2 Comparison
            val cardY2 = 170f
            val timeSavedStr = "${result.timeSavedMonths} months early"
            drawMetadataCard(canvas, 40f, cardY2, cardW, cardH, lightBgPaint, linePaint,
                "Accelerated Months Saved",
                timeSavedStr, bodyPaint, valBigPaint)

            drawMetadataCard(canvas, 215f, cardY2, cardW, cardH, lightBgPaint, linePaint,
                "Accumulated Savings",
                formatCurrency(result.interestSaved, sym), bodyPaint, valBigPaint)

            // Table of Debts Included
            canvas.drawText("Debts Included in Payoff Plan", 40f, 246f, h2Paint)

            val tableY = 262f
            canvas.drawRect(40f, tableY, 555f, tableY + 20f, bgPaint)

            val colsX = floatArrayOf(45f, 190f, 320f, 440f)
            val headerKeys = arrayOf("Debt Name", "Starting balance", "Annual Rate", "Min Payment")

            for (i in headerKeys.indices) {
                canvas.drawText(headerKeys[i], colsX[i], tableY + 13f, headerTextPaint)
            }

            var currY = tableY + 20f
            for (i in debts.indices) {
                val debt = debts[i]
                if (i % 2 == 1) {
                    canvas.drawRect(40f, currY, 555f, currY + 16f, lightBgPaint)
                }
                canvas.drawText(debt.name, colsX[0], currY + 11f, bodyBoldPaint)
                canvas.drawText(formatCurrency(debt.balance, sym), colsX[1], currY + 11f, bodyPaint)
                canvas.drawText("${debt.interestRate} %", colsX[2], currY + 11f, bodyPaint)
                canvas.drawText(formatCurrency(debt.minimumPayment, sym), colsX[3], currY + 11f, bodyPaint)

                canvas.drawLine(40f, currY + 16f, 555f, currY + 16f, linePaint)
                currY += 16f
            }

            // Payoff Schedule Projection Header
            canvas.drawText(Translations.get(TranslationKey.DEBT_PLANNER_TIMELINE, lang), 40f, currY + 22f, h2Paint)

            val sTableY = currY + 32f
            canvas.drawRect(40f, sTableY, 555f, sTableY + 20f, bgPaint)
            val scheCols = floatArrayOf(45f, 180f, 320f, 440f)
            val scheHeaders = arrayOf("Month / Phase", "Remaining Pool", "Interest Charge", "Status")

            for (i in scheHeaders.indices) {
                canvas.drawText(scheHeaders[i], scheCols[i], sTableY + 13f, headerTextPaint)
            }

            currY = sTableY + 20f
            var pageNumber = 1
            var displayCounter = 0

            // Add steps
            for (month in result.monthlyProjection) {
                if (currY + 18f > pageHeight - 65f) {
                    drawFooter(canvas, pageNumber, sym, pageWidth, pageHeight, bodyPaint, linePaint)
                    pdfDocument.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    canvas.drawRect(40f, 40f, 555f, 60f, bgPaint)
                    for (i in scheHeaders.indices) {
                        canvas.drawText(scheHeaders[i], scheCols[i], 53f, headerTextPaint)
                    }
                    currY = 60f
                }

                if (displayCounter % 2 == 1) {
                    canvas.drawRect(40f, currY, 555f, currY + 16f, lightBgPaint)
                }

                val progress = if (month.totalRemainingBalance <= 0.1) "Debt Free! 🎉" else "Amortizing Pool"

                canvas.drawText("Month ${month.monthNumber}", scheCols[0], currY + 11f, bodyBoldPaint)
                canvas.drawText(formatCurrency(month.totalRemainingBalance, sym), scheCols[1], currY + 11f, bodyPaint)
                canvas.drawText(formatCurrency(month.totalInterestPaidThisMonth, sym), scheCols[2], currY + 11f, bodyPaint)
                canvas.drawText(progress, scheCols[3], currY + 11f, if (month.totalRemainingBalance <= 0.1) valBigPaint else bodyPaint)

                canvas.drawLine(40f, currY + 16f, 555f, currY + 16f, linePaint)
                currY += 16f
                displayCounter++
            }

            drawFooter(canvas, pageNumber, sym, pageWidth, pageHeight, bodyPaint, linePaint)
            pdfDocument.finishPage(page)

            // Cache & Share
            val cacheFile = File(context.cacheDir, "debt_payoff_planner_report.pdf")
            val outputStream = FileOutputStream(cacheFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Debt Payoff Planner Report")
                putExtra(Intent.EXTRA_TEXT, "Here is your custom Snowball/Avalanche Debt Payoff Plan.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Debt Payoff PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            return true

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun drawMetadataCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        bg: Paint,
        stroke: Paint,
        label: String,
        value: String,
        lblPaint: Paint,
        valPaint: Paint
    ) {
        canvas.drawRect(x, y, x + w, y + h, bg)
        canvas.drawRect(x, y, x + w, y + h, stroke)
        canvas.drawText(label.uppercase(), x + 8f, y + 15f, lblPaint)
        canvas.drawText(value, x + 8f, y + 35f, valPaint)
    }

    private fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        sym: String,
        width: Int,
        height: Int,
        paint: Paint,
        linePaint: Paint
    ) {
        val y = height - 40f
        canvas.drawLine(40f, y - 10f, 555f, y - 10f, linePaint)
        canvas.drawText("Mortgage & Loan Calculator © 2026 | Page $pageNumber", 40f, y + 4f, paint)
        canvas.drawText("All values estimated.", 460f, y + 4f, paint)
    }

    private fun Double.format(digits: Int) = String.format("%.${digits}f", this)

    private fun formatCurrency(value: Double, symbol: String): String {
        return "$symbol ${String.format("%,.2f", value)}"
    }
}
