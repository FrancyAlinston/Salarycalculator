package com.example.salarycalculator.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.salarycalculator.wear.MainActivity
import com.example.salarycalculator.wear.R
import com.example.salarycalculator.wear.domain.WearDutyState
import com.example.salarycalculator.wear.domain.WearSalaryRepository
import java.time.LocalDate

/**
 * [ACTION: FEAT-928]
 * Watch Face Complication Provider for Samsung Galaxy Watch 8 Classic.
 * Shows whether the current day has duty or not ("Night Duty" on night shift days,
 * "Off Duty" on rest days) without exposing net pay on the watch face.
 */
class WearShiftComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return buildComplicationData(
            type = type,
            statusText = "Night Duty",
            titleText = "Tonight",
            longSubtitle = "20:00 – 08:00 • 12h",
            hasDuty = true,
            tapAction = null
        )
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val repo = WearSalaryRepository(applicationContext)
        val summary = repo.getTodayDutySummary(LocalDate.now())

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val shortTitle = when (summary.dutyState) {
            WearDutyState.NIGHT_DUTY -> "Tonight"
            WearDutyState.OVERTIME -> "Overtime"
            WearDutyState.ANNUAL_LEAVE -> "Leave"
            WearDutyState.SICK_LEAVE -> "Sick"
            WearDutyState.OFF_DUTY -> "Rest Day"
        }

        return buildComplicationData(
            type = request.complicationType,
            statusText = summary.shortStatus, // "Night Duty" or "Off Duty"
            titleText = shortTitle,
            longSubtitle = summary.subtitle,
            hasDuty = summary.hasDuty,
            tapAction = tapPendingIntent
        )
    }

    private fun buildComplicationData(
        type: ComplicationType,
        statusText: String,
        titleText: String,
        longSubtitle: String,
        hasDuty: Boolean,
        tapAction: PendingIntent?
    ): ComplicationData? {
        val iconRes = if (hasDuty) R.drawable.ic_wear_night_duty else R.drawable.ic_wear_off_duty
        val monoImage = MonochromaticImage.Builder(
            Icon.createWithResource(applicationContext, iconRes)
        ).build()

        val plainStatus = PlainComplicationText.Builder(statusText).build()
        val plainTitle = PlainComplicationText.Builder(titleText).build()
        val plainLongSub = PlainComplicationText.Builder(longSubtitle).build()
        val contentDesc = PlainComplicationText.Builder("Today Duty Status: $statusText").build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = plainStatus,
                    contentDescription = contentDesc
                )
                    .setTitle(plainTitle)
                    .setMonochromaticImage(monoImage)
                    .setTapAction(tapAction)
                    .build()
            }

            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = plainStatus,
                    contentDescription = contentDesc
                )
                    .setTitle(plainLongSub)
                    .setMonochromaticImage(monoImage)
                    .setTapAction(tapAction)
                    .build()
            }

            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = if (hasDuty) 100f else 0f,
                    min = 0f,
                    max = 100f,
                    contentDescription = contentDesc
                )
                    .setText(plainStatus)
                    .setTitle(plainTitle)
                    .setMonochromaticImage(monoImage)
                    .setTapAction(tapAction)
                    .build()
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = monoImage,
                    contentDescription = contentDesc
                )
                    .setTapAction(tapAction)
                    .build()
            }

            else -> null
        }
    }
}
