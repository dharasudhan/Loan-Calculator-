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
        loanType: String,
        byMonthly: Boolean = false,
        currencySymbol: String? = null
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

            val sym = currencySymbol ?: lang.currencySymbol

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
            val schedulePeriodTitle = if (byMonthly) {
                Translations.get(TranslationKey.SCHEDULE_TAB, lang) + " (" + Translations.get(TranslationKey.MONTH, lang) + ")"
            } else {
                Translations.get(TranslationKey.SCHEDULE_TAB, lang) + " (" + Translations.get(TranslationKey.YEAR_LABEL, lang) + "ly)"
            }

            canvas.drawText(
                schedulePeriodTitle,
                40f,
                265f,
                h2Paint
            )

            // Draw Table Headers
            val tableY = 285f
            canvas.drawRect(40f, tableY, 555f, tableY + 22f, bgPaint)
            
            val colsX = floatArrayOf(45f, 100f, 180f, 270f, 360f, 455f)
            val headerKeys = arrayOf(
                if (byMonthly) Translations.get(TranslationKey.MONTH, lang) else Translations.get(TranslationKey.YEAR, lang),
                "Total Paid",
                Translations.get(TranslationKey.PRINCIPAL_AND_INTEREST, lang).take(12) + "..",
                Translations.get(TranslationKey.TOTAL_INTEREST, lang).take(12) + "..",
                "Extra Paid",
                if (byMonthly) "Balance" else "Ending Balance"
            )

            for (i in headerKeys.indices) {
                canvas.drawText(headerKeys[i], colsX[i], tableY + 14f, headerTextPaint)
            }

            var currY = tableY + 22f
            var itemCounter = 0
            var pageNumber = 1

            data class ScheduleRow(
                val indexLabel: String,
                val paymentAmount: Double,
                val principalPaid: Double,
                val interestPaid: Double,
                val extraPayment: Double,
                val balance: Double
            )

            val rowsToPrint = if (byMonthly) {
                result.schedule.map { item ->
                    ScheduleRow(
                        indexLabel = item.monthNumber.toString(),
                        paymentAmount = item.paymentAmount,
                        principalPaid = item.principalPaid,
                        interestPaid = item.interestPaid,
                        extraPayment = item.extraPayment,
                        balance = item.remainingBalance
                    )
                }
            } else {
                result.yearlySchedule.map { item ->
                    ScheduleRow(
                        indexLabel = item.yearNumber.toString(),
                        paymentAmount = item.paymentAmount,
                        principalPaid = item.principalPaid,
                        interestPaid = item.interestPaid,
                        extraPayment = item.extraPayment,
                        balance = item.endingBalance
                    )
                }
            }

            // Write Schedule Items
            for (item in rowsToPrint) {
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
                
                canvas.drawText(item.indexLabel, colsX[0], currY + 12f, bodyBoldPaint)
                canvas.drawText(formatCurrency(item.paymentAmount, sym), colsX[1], currY + 12f, bodyPaint)
                canvas.drawText(formatCurrency(item.principalPaid, sym), colsX[2], currY + 12f, bodyPaint)
                canvas.drawText(formatCurrency(item.interestPaid, sym), colsX[3], currY + 12f, bodyPaint)
                canvas.drawText(formatCurrency(item.extraPayment, sym), colsX[4], currY + 12f, bodyPaint)
                canvas.drawText(formatCurrency(item.balance, sym), colsX[5], currY + 12f, bodyBoldPaint)

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

    fun generateAndShareRentVsBuyPdf(
        context: Context,
        lang: LanguageCode,
        rentMonthly: Double,
        rentIncrease: Double,
        rentersIns: Double,
        homePriceVal: Double,
        downPaymentVal: Double,
        interestRateVal: Double,
        loanTermYears: Int,
        taxRateVal: Double,
        homeInsVal: Double,
        maintenanceVal: Double,
        appreciationVal: Double,
        registrationTaxVal: Double,
        breakEvenYear: Int?,
        plannedYears: Int,
        projectionList: List<YearlyComparisonRow>
    ): Boolean {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            // Paints
            val titlePaint = Paint().apply {
                color = Color.parseColor("#0F172A") // Slate 900
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val h2Paint = Paint().apply {
                color = Color.parseColor("#1E293B") // Slate 800
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val valBigPaint = Paint().apply {
                color = Color.parseColor("#0369A1") // Sky Premium 700
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.parseColor("#475569") // Slate 600
                textSize = 8.5f
                isAntiAlias = true
            }

            val bodyBoldPaint = Paint().apply {
                color = Color.parseColor("#1E293B") // Slate 800
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bgPaint = Paint().apply {
                color = Color.parseColor("#0F172A") // Slate 900
                style = Paint.Style.FILL
            }

            val lightBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }

            val infoBgPaint = Paint().apply {
                color = Color.parseColor("#EFF6FF") // Light blue tint
                style = Paint.Style.FILL
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 0.7f
            }

            val sym = lang.currencySymbol

            // PAGE 1: Overview, Dynamic Plan Strategy & Table
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Brand Header Accent Bar
            val accentPaint = Paint().apply {
                color = Color.parseColor("#0284C7") // Sky 600
                style = Paint.Style.FILL
            }
            canvas.drawRect(40f, 35f, 555f, 39f, accentPaint)

            val pdfTitle = when(lang) {
                LanguageCode.ES -> "Análisis Estratégico y Reporte de ROI de Alquiler vs Compra"
                LanguageCode.FR -> "Rapport d'Analyse Stratégique & ROI Louer vs Acheter"
                LanguageCode.DE -> "Strategische Analyse & ROI-Bericht: Mieten vs. Kaufen"
                LanguageCode.HI -> "किराया बनाम खरीद रणनीतिक विश्लेषण और आरओआई रिपोर्ट"
                LanguageCode.TA -> "வாடகை vs கொள்முதல் வியூகம் & ROI அறிக்கை"
                else -> "Rent Vs Buy Strategic Analysis & ROI Report"
            }

            canvas.drawText(
                pdfTitle,
                40f,
                62f,
                titlePaint
            )

            val detailsText = when(lang) {
                LanguageCode.ES -> "Alquiler Mensual: ${formatCurrencyInt(rentMonthly, sym)} (${rentIncrease}%/año) | Precio Casa: ${formatCurrencyInt(homePriceVal, sym)} | Estancia Objetivo: $plannedYears Años"
                LanguageCode.FR -> "Loyer Mensuel: ${formatCurrencyInt(rentMonthly, sym)} (${rentIncrease}%/an) | Prix Maison: ${formatCurrencyInt(homePriceVal, sym)} | Séjour Cible: $plannedYears Ans"
                LanguageCode.DE -> "Monatliche Miete: ${formatCurrencyInt(rentMonthly, sym)} (${rentIncrease}%/Jahr) | Hauspreis: ${formatCurrencyInt(homePriceVal, sym)} | Geplante Dauer: $plannedYears Jahre"
                LanguageCode.HI -> "मासिक किराया: ${formatCurrencyInt(rentMonthly, sym)} (${rentIncrease}%/वर्ष) | मूल्य: ${formatCurrencyInt(homePriceVal, sym)} | रहने की योजना: $plannedYears वर्ष"
                LanguageCode.TA -> "மாத வாடகை: ${formatCurrencyInt(rentMonthly, sym)} (${rentIncrease}%/வருடம்) | வீட்டின் விலை: ${formatCurrencyInt(homePriceVal, sym)} | திட்டமிட்ட காலம்: $plannedYears ஆண்டுகள்"
                else -> "Monthly Rent: ${formatCurrencyInt(rentMonthly, sym)} (${rentIncrease}%/yr) | Home Price: ${formatCurrencyInt(homePriceVal, sym)} | Target Stay: $plannedYears Years"
            }

            val stampDutyText = when(lang) {
                LanguageCode.ES -> "Registro e Impuestos: ${registrationTaxVal}% (Inicial: ${formatCurrencyInt(homePriceVal * (registrationTaxVal / 100.0), sym)}) | Tasa Impuesto: ${taxRateVal}%/año"
                LanguageCode.FR -> "Enregistrement & Taxes: ${registrationTaxVal}% (Initial: ${formatCurrencyInt(homePriceVal * (registrationTaxVal / 100.0), sym)}) | Taux Impôt: ${taxRateVal}%/an"
                LanguageCode.DE -> "Kaufnebenkosten: ${registrationTaxVal}% (Soll: ${formatCurrencyInt(homePriceVal * (registrationTaxVal / 100.0), sym)}) | Steuersatz: ${taxRateVal}%/Jahr"
                LanguageCode.HI -> "पंजीकरण और कर: ${registrationTaxVal}% (अग्रिम: ${formatCurrencyInt(homePriceVal * (registrationTaxVal / 100.0), sym)}) | कर दर: ${taxRateVal}%/वर्ष"
                LanguageCode.TA -> "பதிவு & முத்திரை வரி: ${registrationTaxVal}% (ஆரம்பம்: ${formatCurrencyInt(homePriceVal * (registrationTaxVal / 100.0), sym)}) | சொத்து வரி: ${taxRateVal}%/வருடம்"
                else -> "Registration & Taxes: ${registrationTaxVal}% (Upfront: ${formatCurrencyInt(homePriceVal * (registrationTaxVal / 100.0), sym)}) | Tax Rate: ${taxRateVal}%/yr"
            }

            val infoPaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText(detailsText, 40f, 74f, infoPaint)
            canvas.drawText(stampDutyText, 40f, 85f, infoPaint)

            canvas.drawLine(40f, 93f, 555f, 93f, linePaint)

            // BLOCK 1: Financial Decision Recommendation (95f - 175f)
            val recY = 103f
            val recH = 75f
            canvas.drawRect(40f, recY, 555f, recY + recH, infoBgPaint)
            canvas.drawRect(40f, recY, 555f, recY + recH, linePaint)
            
            val recTitlePaint = Paint().apply {
                color = Color.parseColor("#0369A1")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val recTextPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 8.5f
                isAntiAlias = true
            }

            val statusTitle = if (breakEvenYear != null) {
                when(lang) {
                    LanguageCode.ES -> "RECOMENDACIÓN: ¡COMPRAR ES ESTRATÉGICAMENTE BENEFICIOSO! (Equilibrio en $breakEvenYear Años)"
                    LanguageCode.FR -> "RECOMMANDATION : ACHETER EST FINANCIÈREMENT AVANTAGEUX (Seuil de rentabilité en $breakEvenYear Ans)"
                    LanguageCode.DE -> "EMPFEHLUNG: KAUFEN IST STRATEGISCH VORTEILHAFT (Break-even nach $breakEvenYear Jahren)"
                    LanguageCode.HI -> "सिफारिश: खरीदना रणनीतिक रूप से फायदेमंद है (लाभ की स्थिति $breakEvenYear वर्षों में)"
                    LanguageCode.TA -> "பரிந்துரை: வாங்குவதே உகந்தது (சமநிலை காலம் $breakEvenYear ஆண்டுகள்)"
                    else -> "RECOMMENDATION: BUYING IS STRATEGICALLY BENEFICIAL (Break-even in $breakEvenYear Years)"
                }
            } else {
                when(lang) {
                    LanguageCode.ES -> "RECOMENDACIÓN: ALQUILAR SIGUE SIENDO FINANCIERAMENTE SUPERIOR"
                    LanguageCode.FR -> "RECOMMANDATION : LA LOCATION RESTE FINANCIÈREMENT SUPÉRIEURE"
                    LanguageCode.DE -> "EMPFEHLUNG: MIETEN BLEIBT FINANZIELL VORTEILHAFTER"
                    LanguageCode.HI -> "सिफारिश: वर्तमान में किराए पर रहना आर्थिक रूप से बेहतर है"
                    LanguageCode.TA -> "பரிந்துரை: வாடகையே தொடர்ந்து சிறந்த நிதி லாபத்தை அளிக்கிறது"
                    else -> "RECOMMENDATION: RENTING REMAINS FINANCIALLY SUPERIOR"
                }
            }
            canvas.drawText(statusTitle, 50f, recY + 16f, recTitlePaint)

            val row30 = projectionList.last()
            val reasoningLines = if (breakEvenYear != null) {
                when(lang) {
                    LanguageCode.ES -> listOf(
                        "• Alquilar es óptimo a corto plazo (Años 1 a ${breakEvenYear - 1}) debido al enganche inicial y altos impuestos de registro.",
                        "• Comprar se vuelve superior en el Año $breakEvenYear (Punto de Equilibrio/ROI). Después de esto, la hipoteca construye",
                        "  patrimonio real (${appreciationVal}%/año) en lugar de rentas perdidas. Al Año 30, acumula ${formatCurrencyInt(row30.buyEquity, sym)}",
                        "  en valor de casa libre comparado con ${formatCurrencyInt(row30.rentCumulativeSpend, sym)} gastados en alquiler acumulado."
                    )
                    LanguageCode.FR -> listOf(
                        "• Louer est idéal à court terme (Ans 1 à ${breakEvenYear - 1}) à cause de l'apport initial et des frais d'enregistrement.",
                        "• L'achat devient rentable à l'Année $breakEvenYear (Seuil ROI). Ensuite, le prêt amorti se convertit en capital",
                        "  revalorisé à ${appreciationVal}%/an. À l'An 30, vous créez un patrimoine net de ${formatCurrencyInt(row30.buyEquity, sym)}",
                        "  comparé à ${formatCurrencyInt(row30.rentCumulativeSpend, sym)} jetés en loyers cumulés."
                    )
                    LanguageCode.DE -> listOf(
                        "• Mieten ist kurzfristig optimal (Jahre 1 bis ${breakEvenYear - 1}) aufgrund hoher Kaufnebenkosten und Anzahlung.",
                        "• Kaufen wird ab Jahr $breakEvenYear (ROI-Punkt) vorteilhafter. Danach wandelt sich Ihre Rate in wachsendes",
                        "  Eigenkapital (${appreciationVal}%/Jahr) statt reiner Mietausgaben. Nach 30 Jahren besitzen Sie ein schuldenfreies",
                        "  Hausvermögen von ${formatCurrencyInt(row30.buyEquity, sym)} im Vergleich zu ${formatCurrencyInt(row30.rentCumulativeSpend, sym)} Mietausgaben."
                    )
                    LanguageCode.HI -> listOf(
                        "• प्रारंभिक डाउनपेमेंट और पंजीकरण शुल्क के कारण अल्पावधि (वर्ष 1 से ${breakEvenYear - 1}) में किराया बेहतर है।",
                        "• वर्ष $breakEvenYear में खरीदना बेहतर हो जाता है। अब मासिक भुगतान गृह संपत्ति के संचय (आकलन ${appreciationVal}%/वर्ष)",
                        "  में परिवर्तित हो जाता है। 30वें वर्ष तक, किराए में ${formatCurrencyInt(row30.rentCumulativeSpend, sym)} खर्च करने के",
                        "  बजाय आपके पास ${formatCurrencyInt(row30.buyEquity, sym)} मूल्य की ऋण-मुक्त संपत्ति होगी।"
                    )
                    LanguageCode.TA -> listOf(
                        "• முன்பணம் மற்றும் இதர ஆரம்ப செலவுகள் காரணமாக ஆரம்ப காலத்தில் (ஆண்டு 1 முதல் ${breakEvenYear - 1}) வாடகையே உகந்தது.",
                        "• $breakEvenYear ஆம் ஆண்டு வாங்குவது சிறந்தது. அதன்பிறகு செலுத்தப்படும் தொகையானது வீட்டின் மதிப்பை ${appreciationVal}%/வருடம்",
                        "  உயர்த்தும். 30 வது முடிவில், வாடகைக்காக ${formatCurrencyInt(row30.rentCumulativeSpend, sym)} வீணாவதற்குப் பதிலாக",
                        "  உங்களுக்கு ${formatCurrencyInt(row30.buyEquity, sym)} மதிப்புள்ள சொந்த வீடு இருக்கும்."
                    )
                    else -> listOf(
                        "• Renting is optimal short term (Yrs 1 to ${breakEvenYear - 1}) due to upfront downpayment and high transactional fees.",
                        "• Buying becomes superior at Year $breakEvenYear (ROI Point). Beyond this, your paid mortgage converts into growing",
                        "  home equity (${appreciationVal}%/yr) instead of unrecoverable payments. By Year 30, you build ${formatCurrencyInt(row30.buyEquity, sym)}",
                        "  in clear home wealth compared to ${formatCurrencyInt(row30.rentCumulativeSpend, sym)} in dead rental expenses."
                    )
                }
            } else {
                when(lang) {
                    LanguageCode.ES -> listOf(
                        "• Alquilar protege el capital durante 30 años porque comprar es demasiado costoso o la apreciación es baja.",
                        "• El alto interés (${interestRateVal}%), impuestos recurrentes (${taxRateVal}%) y mantenimiento (${maintenanceVal}%) superan la apreciación.",
                        "• En el transcurso de 30 años, alquilar sigue siendo la decisión financiera correcta bajo estas condiciones."
                    )
                    LanguageCode.FR -> listOf(
                        "• Louer protège le capital sur 30 ans car les frais d'acquisition sont élevés ou la revalorisation est trop lente.",
                        "• Le fort taux d'intérêt (${interestRateVal}%), les taxes régulières (${taxRateVal}%) et frais d'entretien (${maintenanceVal}%) dégradent l'actif.",
                        "• Sur une période de 30 ans, rester locataire est la stratégie financière la plus protectrice ici."
                    )
                    LanguageCode.DE -> listOf(
                        "• Mieten schützt Ihr Kapital über 30 Jahre hinweg, da die Erwerbs- und Nebenkosten hoch oder die Wertsteigerung gering sind.",
                        "• Hohe Kreditzinsen (${interestRateVal}%), Grundsteuer (${taxRateVal}%) und Instandhaltung (${maintenanceVal}%) dämmen Wertzuwächse ein.",
                        "• Bei diesen Einstellungen ist Mieten über den gesamten Zeitraum von 30 Jahren die wirtschaftlich klügere Entscheidung."
                    )
                    LanguageCode.HI -> listOf(
                        "• किराया 30 वर्षों तक पूंजी की रक्षा करता है क्योंकि खरीदने के खर्च अत्यधिक हैं या गृह मूल्य वृद्धि बहुत कम है।",
                        "• उच्च ऋण ब्याज दर (${interestRateVal}%), आवर्ती कर (${taxRateVal}%), और रखरखाव शुल्क (${maintenanceVal}%) लाभ को समाप्त करते हैं।",
                        "• 30 वर्षों की अवधि में, इन सेटिंग्स के तहत किराए पर रहना ही सही वित्तीय रणनीति है।"
                    )
                    LanguageCode.TA -> listOf(
                        "• கூடுதல் வாங்குதல் கட்டணங்கள் அல்லது குறைந்த வீட்டின் மதிப்பு உயர்வால் 30 ஆண்டுகளில் வாடகையே தலைநகரைக் காக்கும்.",
                        "• உயர் வட்டி விகிதம் (${interestRateVal}%), சொத்து வரி (${taxRateVal}%) மற்றும் பராமரிப்புக் கட்டணம் (${maintenanceVal}%) வளர்ச்சியைத் தடுக்கும்.",
                        "• 30 ஆண்டுகால வியூகத்தில், தொடர்ந்து வாடகைக்கு இருப்பதே சரியான நிதித் தேர்வாகும்."
                    )
                    else -> listOf(
                        "• Renting protects capital over 30 years because buying costs are elevated or appreciation is too low.",
                        "• High loan interest rate (${interestRateVal}%), recurring taxes (${taxRateVal}%), and upkeep fees (${maintenanceVal}%) outpace",
                        "  potential equity. Over the span of 30 years, renting remains the correct financial strategy under these settings."
                    )
                }
            }

            var textY = recY + 28f
            for (line in reasoningLines) {
                canvas.drawText(line, 50f, textY, recTextPaint)
                textY += 11f
            }

            // BLOCK 2: Personalized Stay Planning Feedback (185f - 245f)
            val planY = 185f
            val planH = 50f
            canvas.drawRect(40f, planY, 555f, planY + planH, lightBgPaint)
            canvas.drawRect(40f, planY, 555f, planY + planH, linePaint)

            val planTitlePaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val personalizeTitle = when(lang) {
                LanguageCode.ES -> "ANÁLISIS PERSONALIZADO PARA SU ESTANCIA DE $plannedYears AÑOS"
                LanguageCode.FR -> "ANALYSE PERSONNALISÉE POUR VOTRE SÉJOUR DE $plannedYears ANS"
                LanguageCode.DE -> "INDIVIDUELLE ANALYSE FÜR IHRE PLANZEIT ($plannedYears JAHRE)"
                LanguageCode.HI -> "रहने की योजनाबद्ध अवधि ($plannedYears वर्ष) के लिए व्यक्तिगत विश्लेषण"
                LanguageCode.TA -> "திட்டமிட்ட உங்களது காலகட்டத்திற்கு ($plannedYears ஆண்டுகள்) தனிப்பயனாக்கப்பட்ட பகுப்பாய்வு"
                else -> "PERSONALIZE ANALYSIS FOR YOUR INTENDED STAY ($plannedYears YEARS)"
            }
            canvas.drawText(personalizeTitle, 50f, planY + 15f, planTitlePaint)

            val isBuyingBetterForPlannedStay = breakEvenYear != null && plannedYears >= breakEvenYear
            val feedbackText = if (isBuyingBetterForPlannedStay) {
                when(lang) {
                    LanguageCode.ES -> "✔ GRAN TIEMPO: ¡Su estancia planeada ($plannedYears años) alcanza/supera el Año de Equilibrio ($breakEvenYear)! Comprar es recomendado."
                    LanguageCode.FR -> "✔ EXCELLENTE CHRONOLOGIE : Votre séjour prévu ($plannedYears ans) atteint ou dépasse le seuil de rentabilité ($breakEvenYear ans). Acheter est recommandé."
                    LanguageCode.DE -> "✔ SEHR GUTE PLANUNG: Ihre Wohndauer ($plannedYears J.) erreicht/übertrifft die Gewinnschwelle ($breakEvenYear J.). Kauf wird empfohlen."
                    LanguageCode.HI -> "✔ उत्कृष्ट समयरेखा: आपकी नियोजित अवधि ($plannedYears वर्ष) ब्रेक-ईवन वर्ष ($breakEvenYear) से अधिक है। खरीदने की सिफारिश की जाती है।"
                    LanguageCode.TA -> "✔ சிறந்த காலம்: திட்டமிட்ட காலம் ($plannedYears ஆண்டுகள்) உங்களது சமநிலை ஆண்டை ($breakEvenYear வரம்பை) பூர்த்தி செய்வதால் வாங்குவது சிறந்தது."
                    else -> "✔ GREAT TIMELINE: Your planned stay ($plannedYears yrs) meets/exceeds the Break-Even Year ($breakEvenYear). Buying is recommended."
                }
            } else if (breakEvenYear != null) {
                when(lang) {
                    LanguageCode.ES -> "❌ ESTANCIA DEMASIADO CORTA: Su estancia de $plannedYears años es menor al Año de Equilibrio ($breakEvenYear). Alquilar es más conveniente."
                    LanguageCode.FR -> "❌ SÉJOUR PRÉVU TROP COURT : Votre séjour ($plannedYears ans) est inférieur au seuil de rentabilité ($breakEvenYear ans). Louer est plus avantageux."
                    LanguageCode.DE -> "❌ ZU KURZE WOHNDAUER: Ihre geplante Dauer ($plannedYears J.) ist kürzer als die Gewinnschwelle ($breakEvenYear J.). Mieten ist finanziell ratsamer."
                    LanguageCode.HI -> "❌ संक्षिप्त नियोजित अवधि: आपकी नियोजित अवधि ($plannedYears वर्ष) ब्रेक-ईवन वर्ष ($breakEvenYear) से कम है। किराया अधिक लाभप्रद है।"
                    LanguageCode.TA -> "❌ குறைந்த காலம்: திட்டமிட்ட காலம் ($plannedYears ஆண்டுகள்) உங்களது சமநிலை ஆண்டைவிட ($breakEvenYear-ஐ விட) குறைவாக இருப்பதால் வாடகையே உகந்தது."
                    else -> "❌ SHORT INTENDED STAY: Your stay ($plannedYears yrs) is shorter than the Break-Even Year ($breakEvenYear). Renting is likely more beneficial."
                }
            } else {
                when(lang) {
                    LanguageCode.ES -> "✔ INQUILINO ÓPTIMO: Debido a que comprar no alcanza equilibrio rentable, alquilar protege su capital líquido."
                    LanguageCode.FR -> "✔ LOCATAIRE OPTIMAL : Puisque l'achat n'est jamais rentable ici, louer est la meilleure décision pour vos liquidités."
                    LanguageCode.DE -> "✔ OPTIMALER MIETER: Da Kaufen hier keine Gewinnschwelle erreicht, schützt Mieten Ihr liquides Kapital."
                    LanguageCode.HI -> "✔ इष्टतम किरायेदार: चूंकि इस स्थिति में खरीद का कोई ब्रेक-ईवन नहीं है, आपकी किराए पर रहने की योजना तरल पूंजी संचय में सहायक है।"
                    LanguageCode.TA -> "✔ வாடகைதாரர் சாதகம்: இந்த நிலையில் வாங்குதல் மூலம் சமநிலை வராது என்பதால், வாடகையில் இருப்பது உங்களது சேமிப்பைப் பாதுகாக்கும்."
                    else -> "✔ OPTIMAL TENANT: Since buying yields no positive break-even here, your plan to rent protects liquid capital."
                }
            }
            val feedbackPaint = Paint().apply {
                color = if (isBuyingBetterForPlannedStay) Color.parseColor("#15803D") else Color.parseColor("#B91C1C")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(feedbackText, 50f, planY + 28f, feedbackPaint)

            val disclaimerText = when(lang) {
                LanguageCode.ES -> "Vender la casa antes del punto de equilibrio conlleva altas comisiones inmobiliarias e impuestos, eliminando el capital acumulado."
                LanguageCode.FR -> "Revendre le bien trop tôt entraîne d'importants frais de courtage et de mutation, effaçant le capital accumulé."
                LanguageCode.DE -> "Ein Verkauf der Immobilie vor Erreichen der Gewinnschwelle verursacht hohe Maklergebühren und Nebenkosten, die das Eigenkapital mindern."
                LanguageCode.HI -> "ब्रेक-ईवन अवधि से पहले घर बेचने पर उच्च ब्रोकरेज और बिक्री कर लगते हैं, जो संचित पूंजी को समाप्त कर देते हैं।"
                LanguageCode.TA -> "சமநிலை காலத்திற்கு முன்னரே வீட்டை விற்க நேர்ந்தால், விற்பனை தரகு மற்றும் இதர வரிகள் உங்களது சேமிப்பை முழுமையாக அழித்துவிடும்."
                else -> "Exiting your home earlier than the break-even span incurs severe sales commissions & fees, wiping out accumulated equity."
            }
            canvas.drawText(disclaimerText, 50f, planY + 40f, bodyPaint)

            // Metadata Cards (245f - 295f)
            val cardY = 245f
            val cardW = 160f
            val cardH = 44f

            val miniLblPaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val card1Label = when(lang) {
                LanguageCode.ES -> "Renta 30 años Gasto Tot."
                LanguageCode.FR -> "Location 30 ans Dépenses"
                LanguageCode.DE -> "Miete 30J Gesamtausgaben"
                LanguageCode.HI -> "किराया 30 वर्ष संचयी व्यय"
                LanguageCode.TA -> "வாடகை 30ஆண்டு மொத்த செலவு"
                else -> "Renter 30yr Cum. Spend"
            }
            val card2Label = when(lang) {
                LanguageCode.ES -> "Comprar 30 años Cost. Net."
                LanguageCode.FR -> "Achat 30 ans Coût Net"
                LanguageCode.DE -> "Kauf 30J Nettokosten"
                LanguageCode.HI -> "खरीद 30 वर्ष कुल शुद्ध लागत"
                LanguageCode.TA -> "வாங்குதல் 30ஆண்டு நிகர செலவு"
                else -> "Buyer 30yr Net Cost"
            }
            val card3Label = when(lang) {
                LanguageCode.ES -> "Comprar 30 años Cap. Acum."
                LanguageCode.FR -> "Achat 30 ans Capital Acquis"
                LanguageCode.DE -> "Kauf 30J Eigenkapital"
                LanguageCode.HI -> "खरीद 30 वर्ष संचित इक्विटी"
                LanguageCode.TA -> "வாங்குதல் 30ஆண்டு மொத்த பங்கு"
                else -> "Buyer 30yr Acquired Equity"
            }

            drawMetadataCard(canvas, 40f, cardY, cardW, cardH, lightBgPaint, linePaint,
                card1Label,
                formatCurrencyInt(row30.rentCumulativeSpend, sym), miniLblPaint, valBigPaint)

            drawMetadataCard(canvas, 215f, cardY, cardW, cardH, lightBgPaint, linePaint,
                card2Label,
                formatCurrencyInt(row30.buyNetCost, sym), miniLblPaint, valBigPaint)

            drawMetadataCard(canvas, 390f, cardY, cardW, cardH, lightBgPaint, linePaint,
                card3Label,
                formatCurrencyInt(row30.buyEquity, sym), miniLblPaint, valBigPaint)

            // Year-by-year projections (305f)
            val comparativeTitle = when(lang) {
                LanguageCode.ES -> "Proyección comparada de riqueza y margen de equilibrio de inversión (ROI)"
                LanguageCode.FR -> "Projection Comparative des Richesse & Horizon de Rentabilité (ROI)"
                LanguageCode.DE -> "Entwicklung des Gesamtvermögens & Gewinnschwelle (ROI-Horizont)"
                LanguageCode.HI -> "अनुमानित तुलनात्मक संपत्ति संतुलन और निवेश पर प्रतिफल (ROI) समयसीमा"
                LanguageCode.TA -> "மதிப்பிடப்பட்ட ஒப்பிடுதல் நிதி நிலைமை & ROI முன்கணிப்பு"
                else -> "Projected Comparative Wealth Balances & ROI Horizon"
            }
            canvas.drawText(comparativeTitle, 40f, 311f, h2Paint)

            val tableY = 322f
            canvas.drawRect(40f, tableY, 555f, tableY + 18f, bgPaint)

            // Adjusted columns perfectly to avoid overlapping and clipping
            val colsX = floatArrayOf(45f, 115f, 220f, 325f, 440f)
            val headerKeys = when(lang) {
                LanguageCode.ES -> arrayOf("Año / Estado", "Rent. Acum.", "Cost. Comp.", "Valor Prop.", "Capital Acum.")
                LanguageCode.FR -> arrayOf("Année / Statut", "Loyers Cum.", "Achat Coût", "Valeur Bien", "Capital Acq.")
                LanguageCode.DE -> arrayOf("Jahr / Status", "Miete Kum.", "Kauf Netto", "Immobilienwert", "Eigenkapital")
                LanguageCode.HI -> arrayOf("वर्ष / स्थिति", "किराया संचयी", "खरीद शुद्ध लागत", "घर का मूल्य", "संचित इक्विटी")
                LanguageCode.TA -> arrayOf("ஆண்டு / நிலை", "வாடகை செலவு", "வாங்குதல் செலவு", "வீட்டின் மதிப்பு", "சொந்த பங்கு")
                else -> arrayOf("Year / Status", "Cum. Rent Cost", "Cum. Buy Cost", "Property Value", "Home Equity")
            }

            for (i in headerKeys.indices) {
                canvas.drawText(headerKeys[i], colsX[i], tableY + 12f, headerTextPaint)
            }

            var currY = tableY + 18f
            val selectedYears = listOf(1, 2, 3, 5, 7, 10, 15, 20, 25, 30)
            
            for (i in selectedYears.indices) {
                val yr = selectedYears[i]
                val item = projectionList.firstOrNull { it.year == yr } ?: continue
                
                if (i % 2 == 1) {
                    canvas.drawRect(40f, currY, 555f, currY + 15f, lightBgPaint)
                }
                
                val highlightPaint = if (item.isBreakEven) valBigPaint else bodyPaint
                val isYourCurrentStay = yr == plannedYears
                val marker = when {
                    isYourCurrentStay && item.isBreakEven -> when(lang) {
                        LanguageCode.ES -> " *Estancia (ROI)"
                        LanguageCode.FR -> " *Séjour (ROI)"
                        LanguageCode.DE -> " *Wohndauer (ROI)"
                        LanguageCode.HI -> " *रहने की अवधि (ROI)"
                        LanguageCode.TA -> " *காலம் (ROI)"
                        else -> " *Stay (ROI)"
                    }
                    isYourCurrentStay -> when(lang) {
                        LanguageCode.ES -> " *Estancia Obj."
                        LanguageCode.FR -> " *Séjour Cible"
                        LanguageCode.DE -> " *Geplante Dauer"
                        LanguageCode.HI -> " *लक्ष्य रहने की अवधि"
                        LanguageCode.TA -> " *திட்டமிட்ட காலம்"
                        else -> " *Target Stay"
                    }
                    item.isBreakEven -> " (ROI)"
                    else -> ""
                }

                canvas.drawText("Yr $yr$marker", colsX[0], currY + 10.5f, bodyBoldPaint)
                canvas.drawText(formatCurrencyInt(item.rentCumulativeSpend, sym), colsX[1], currY + 10.5f, bodyPaint)
                canvas.drawText(formatCurrencyInt(item.buyOutPocketSpend, sym), colsX[2], currY + 10.5f, bodyPaint)
                canvas.drawText(formatCurrencyInt(item.buyHomeValue, sym), colsX[3], currY + 10.5f, bodyPaint)
                canvas.drawText(formatCurrencyInt(item.buyEquity, sym), colsX[4], currY + 10.5f, highlightPaint)

                canvas.drawLine(40f, currY + 15f, 555f, currY + 15f, linePaint)
                currY += 15f
            }

            // Glossary and Financial Abbreviations Block (505f to 760f)
            val glossY = currY + 15f
            val glossH = 180f
            
            canvas.drawRect(40f, glossY, 555f, glossY + glossH, lightBgPaint)
            canvas.drawRect(40f, glossY, 555f, glossY + glossH, linePaint)

            val glossTitlePaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val glossTitle = when(lang) {
                LanguageCode.ES -> "EXPLICACIÓN Y DICCIONARIO DE ABREVIACIONES FINANCIERAS"
                LanguageCode.FR -> "DICTIONNAIRE ET EXPLICATION DES TERMES FINANCIERS"
                LanguageCode.DE -> "ERKLÄRUNG & DOKUMENTATION DER FINANZ-ABKÜRZUNGEN"
                LanguageCode.HI -> "वित्तीय संक्षिप्ताक्षरों की व्याख्या एवं शब्दावली व्याख्या"
                LanguageCode.TA -> "நிதிச் சொற்கள் & குறுக்கங்களின் விளக்க உரை"
                else -> "EXPLANATION & DICTIONARY OF FINANCIAL ABBREVIATIONS"
            }
            canvas.drawText(glossTitle, 50f, glossY + 16f, glossTitlePaint)

            val glossaryLines = when(lang) {
                LanguageCode.ES -> listOf(
                    "• ROI (Retorno de Inversión / Equilibrio): El año en el que comprar supera a alquilar. Pasado este umbral, el capital creado",
                    "  por amortización y apreciación vence a la inflación del alquiler. Fuera de esto, alquilar conserva más capital.",
                    "• REG_TAX (Impuestos de Registro %): Derechos del registro de propiedad y tasas iniciales cobradas por el gobierno.",
                    "  Se calcula como un costo de transacción inicial directo, incrementando el capital de compra inicial necesario.",
                    "• HOA (Asociación de Propietarios): Cuotas de mantenimiento obligatorias cobradas a los propietarios de viviendas.",
                    "  Suma cargos fijos adicionales no recuperables a las finanzas mensuales de su escenario de propiedad de vivienda.",
                    "• LTV (Relación Préstamo-Valor): El porcentaje restante de la hipoteca calculado como: Monto del préstamo / Valor de la vivienda.",
                    "  Si el enganche inicial es inferior al 20% (LTV > 80%), las instituciones bancarias suelen requerir el cobro mensual del PMI.",
                    "• PMI (Seguro Hipotecario Privado): Prima mensual de seguridad para proteger al prestamista por enganche bajo.",
                    "  No contribuye a la reducción del saldo de su hipoteca, restando competitividad inicial a la compra.",
                    "• APR (Efectivo Anual %): El costo hipotecario completo anualizado. Combina la tasa de interés base con honorarios de apertura",
                    "  y costos obligatorios de cierre. Compare siempre el APR para medir el punto exacto de rentabilidad financiera."
                )
                LanguageCode.FR -> listOf(
                    "• ROI (Seuil de Rentabilité) : Date à laquelle l'acheteur devient plus riche que le locataire. Après cette ligne, l'apport",
                    "  de la valorisation immobilière et remboursement d'emprunt dépasse l'inflation des loyers.",
                    "• REG_TAX (Enregistrement & Mutation %) : Droits de timbre officiels et impôts d'acquisition perçus par l'État lors de l'achat.",
                    "  Ces frais initiaux non-récupérables augmentent considérablement l'apport net initial requis.",
                    "• HOA (Frais de Copropriété) : Cotisations récurrentes obligatoires payées pour l'entretien et services des parties communes.",
                    "  Cela représente de purs coûts mensuels additionnels non-récupérables sur votre scénario d'acquisition.",
                    "• LTV (Ratio Prêt/Valeur) : Ratio mesurant le prêt immobilier comparé au prix d'achat, soit : Encours du Prêt / Prix du Bien.",
                    "  Si votre apport est inférieur à 20% (LTV > 80%), la banque exige généralement des frais supplémentaires périodiques (PMI).",
                    "• PMI (Assurance Emprunteur) : Assurance obligatoire exigée par les prêteurs pour les emprunts à faible apport personnel.",
                    "  Ceci s'ajoute au coût restant sans amortir votre capital d'emprunt, ce qui rend la location temporaire plus attractive.",
                    "• APR (Taux Effectif Global) : Coût annuel global réel de votre financement, incluant le taux brut, primes de courtage,",
                    "  commissions et amortissements de dossier. Servez-vous de l'APR pour localiser précisément le croisement ROI."
                )
                LanguageCode.DE -> listOf(
                    "• ROI (Kapitalrendite): Der Zeitpunkt, an dem das Wohneigentum wirtschaftlicher wird als das Mieten. Über dieses Jahr hinaus",
                    "  übersteigen Wertwachstum & Tilgung die Mietinflation. Wenn Sie vor diesem Jahr ausziehen, war Mieten die bessere Wahl.",
                    "• REG_TAX (Kaufnebenkosten %): Staatliche Grunderwerbsteuer, Notargebühren und Grundbucheintragung beim Erwerb.",
                    "  Einmalige Transaktionskosten, die das erforderliche Anfangskapital zum Immobilienkauf direkt anheben.",
                    "• HOA (Hausbesitzervereinigung / Hausgeld): Zwingende Umlagen zur Instandhaltung von Gemeinschaftsflächen und Gebäuden.",
                    "  Stellt wiederkehrende, nicht erstattungsfähige Zusatzkosten dar, die in Ihrem Kaufthema anfallen.",
                    "• LTV (Beleihungsauslauf %): Das prozentuale Verhältnis Ihres Kredits zum Kaufpreis der Immobilie (Kreditbetrag / Hauspreis).",
                    "  Bei einer Anzahlung unter 20 % (LTV > 80 %) verlangen Banken in der Regel den Abschluss einer privaten Versicherung (PMI).",
                    "• PMI (Private Hypothekenversicherung): Sicherheitsprämie für Banken bei Darlehen mit geringem Eigenkapital.",
                    "  Ist eine reine monatliche Gebühr, die Ihre Restschuld nicht tilgt und Mieten anfangs wettbewerbsfähiger macht.",
                    "• APR (Effektiver Jahreszins %): Die echten, jährlichen Gesamtkosten Ihrer Finanzierung inklusive aller Nebengebühren",
                    "  und Abschlusskosten. Nutzen Sie immer den APR, um die Gewinnschwelle der Rentabilität zu ermitteln."
                )
                LanguageCode.HI -> listOf(
                    "• आरओआई (निवेश पर रिटर्न): वह सटीक वर्ष जब खरीदार किराएदार से अधिक अमीर हो जाता है। इस वर्ष के बाद,",
                    "  संपत्ति वृद्धि और ऋण भुगतान किराए की वृद्धि दर को मात देते हैं। यदि आप इससे पहले घर बेचते हैं, तो किराया अधिक लाभदायक है।",
                    "• पंजीकरण और कर % (REG_TAX): सरकारी स्टांप शुल्क, पंजीकरण शुल्क और संपत्ति खरीद के दौरान लिया जाने वाला अग्रिम कर।",
                    "  यह एक अग्रिम लेनदेन लागत है जो घर खरीदने के लिए आवश्यक प्रारंभिक धनराशि को बढ़ा देती है।",
                    "• एचओए (गृहस्वामी संघ): संपत्ति के मालिकों द्वारा किया जाने वाला अनिवार्य भुगतान। यह साझा स्थानों के रखरखाव में उपयोगी है,",
                    "  परंतु यह आपके खरीद के स्वामित्व वित्तीय मॉडल पर अतिरिक्त निश्चित गैर-वसूलने योग्य खर्च बढ़ाता है।",
                    "• एलटीवी (ऋण-से-मूल्य अनुपात): घर की कीमत के सापेक्ष गृह ऋण का प्रतिशत। इसकी गणना ऋण राशि / घर की कीमत के रूप में की जाती है।",
                    "  यदि आपका डाउन पेमेंट 20% से कम है (LTV > 80%), तो ऋणदाता आमतौर पर आपसे एक पीएमआई बीमा लेने की मांग करते हैं।",
                    "• पीएमआई (निजी बंधक बीमा): कम डाउन पेमेंट वाले उधारकर्ताओं से लिया जाने वाला एक अनिवार्य सुरक्षा प्रीमियम।",
                    "  यह बैंक के लिए सुरक्षा मात्र है और मूल ऋण राशि को नहीं घटाता, जिससे प्रारंभ में किराया अधिक प्रतिस्पर्धी रहता है।",
                    "• एपीआर (वास्तविक वार्षिक दर %): वित्तपोषण की वास्तविक व्यापक वार्षिक लागत। इसमें नाममात्र ब्याज दर के साथ ऋण प्रबंधन शुल्क",
                    "  और समापन लागत शामिल हैं। सटीक ब्रेक-ईवन वर्ष की गणना के लिए हमेशा एपीआर का ही उपयोग करें।"
                )
                LanguageCode.TA -> listOf(
                    "• ROI (முதலீட்டின் மீதான லாபம்/சமநிலை): வாடகைக்கு இருப்பதைவிட வாங்குபவர் அதிக லாபம் பெறும் ஆண்டைக் குறிக்கிறது.",
                    "  இவ்வாண்டிற்குப் பிறகு வீட்டின் மதிப்பு உயர்வு மற்றும் கடன் சரிவு வாடகை உயர்வை முறியடிக்கும்; அதற்கு முன்னர் வாடகையே சிறந்தது.",
                    "• REG_TAX (பதிவு மற்றும் வரிகள் %): சொத்தை வாங்கும் போது செலுத்தப்பட வேண்டிய அரசு முத்திரைத்தாள் மற்றும் பத்திரப்பதிவுக் கட்டணம்.",
                    "  இது வாங்குதலின் ஆரம்பத் தேவையை மற்றும் தேவையான தொடக்கப் பணம் மதிப்பை உயர்த்தும் கட்டாய ஆரம்பச் செலவாகும்.",
                    "• HOA (வீட்டு உரிமையாளர் சங்கம்): சொத்து உரிமையாளர்களால் செலுத்தப்படும் தொடர் கட்டணம். இது பொது இடங்களைப் பராமரிக்க",
                    "  உதவுகிறது, ஆனால் உங்களது மாதாந்திர உரிமையாளர் செலவுகளில் திரும்பப் பெற முடியாத கூடுதல் நிலையான செலவாகிறது.",
                    "• LTV (கடன்-மதிப்பு விகிதம்): வீட்டின் விலைக்கு நிகரான உங்களது கடன் தொகையின் சதவீதம் (கடன் தொகை / வீட்டின் விலை).",
                    "  முன்பணம் 20%-க்கு குறைவாக இருந்தால் (LTV > 80%), கடன் வழங்குநர்கள் உங்களை காப்பீடு (PMI) செய்யக் கோருவார்கள்.",
                    "• PMI (தனியார் அடமானக் காப்பீடு): குறைந்த கடன்தொகை முன்பணம் கொண்டவர்களுக்கு வசூலிக்கப்படும் கட்டாயப் பாதுகாப்பு பிரீமியம்.",
                    "  இது உங்களது அசல் கடனைக் குறைக்க உதவாது, எனவே ஆரம்பத்தில் வாடகையே அதிக நிதி நெகிழ்வுத்தன்மையை வழங்குகிறது.",
                    "• APR (வருடாந்திர வட்டி விகிதம் %): கடனுக்கான உண்மையான வருடாந்திர செலவு. இது வட்டி விகிதத்துடன் இதர எல்லாக் கோப்பு கட்டணங்களையும் உள்ளடக்கியது.",
                    "  சமநிலை ஆண்டை துல்லியமாகக் கணக்கிட எப்போதும் APR-ஐயே ஒப்பிட்டுப் பார்க்க வேண்டும்."
                )
                else -> listOf(
                    "• ROI (Return on Investment): Point in time where the net buyer gets richer than renting. Beyond this year, the property",
                    "  growth & debt paydown beat rent inflation. If you exit the home before the ROI year, renting is financially superior.",
                    "• REG_TAX (Registration & Purchase Taxes %): Government stamp duties, registration fees, and upfront taxes charged during",
                    "  property acquisition. Charged as an upfront purchase transactional cost that increases required buying starting capital.",
                    "• HOA (Homeowners Association): Compulsory dues paid by property owners. Helps maintain neighborhood and building standards,",
                    "  shared utilities, but increases fixed, non-recoverable out-of-pocket costs on your ownership scenario.",
                    "• LTV (Loan-To-Value Ratio): The percentage of mortgage loan relative to home price. Calculated as Loan Amount / Home Price.",
                    "  If your down payment is less than 20% (LTV > 80%), lenders usually require you to buy Private Mortgage Insurance.",
                    "• PMI (Private Mortgage Insurance): Protection premium charged to buyers who represent higher lending risks. It is paid as",
                    "  an extra monthly cost that does not contribute to your mortgage principal balance, making renting more competitive initially.",
                    "• APR (Annual Percentage Rate): The yearly real cost of financing. It accounts for coupon interest, mortgage points, broker",
                    "  commissions, and setup fees. Always check APR to determine the precise ROI crossover year in Rent vs Buy calculations."
                )
            }

            var glossTextY = glossY + 30f
            val miniTextPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 7.5f
                isAntiAlias = true
            }
            
            for (line in glossaryLines) {
                canvas.drawText(line, 50f, glossTextY, miniTextPaint)
                glossTextY += 11f
            }

            // Draw Final Page Footer
            drawFooter(canvas, 1, sym, pageWidth, pageHeight, bodyPaint, linePaint)
            pdfDocument.finishPage(page)

            // Save PDF to cache dir & share it
            val cacheFile = File(context.cacheDir, "rent_vs_buy_projections.pdf")
            val outputStream = FileOutputStream(cacheFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()

            // Share URI
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Strategic Rent Vs Buy Analysis")
                putExtra(Intent.EXTRA_TEXT, "Here is your custom Rent vs Buy comparative strategic decision and financial report PDF.")
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

    private fun formatCurrencyInt(value: Double, symbol: String): String {
        return "$symbol ${String.format("%,.0f", value)}"
    }

    private fun Double.format(digits: Int) = String.format("%.${digits}f", this)

    private fun formatCurrency(value: Double, symbol: String): String {
        return "$symbol ${String.format("%,.2f", value)}"
    }
}
