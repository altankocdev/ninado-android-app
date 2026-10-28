package com.altankoc.nidado.util.notification

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.altankoc.nidado.R

data class NotificationSound(
    val id: String,
    val label: String,
    val resId: Int,
    val icon: ImageVector
)

object NotificationSounds {
    val ALL = listOf(
        NotificationSound("thunder", "Gök Gürültüsü", R.raw.notif_thunder, Icons.Rounded.Bolt),
        NotificationSound("rain", "Yağmur", R.raw.notif_rain_drop, Icons.Rounded.WaterDrop),
        NotificationSound("bird", "Kuş Cıvıltısı", R.raw.notif_bird, Icons.Rounded.MusicNote),
        NotificationSound("storm", "Fırtına", R.raw.notif_storm, Icons.Rounded.Cloud),
        NotificationSound("ocean_waves", "Okyanus Dalgası", R.raw.notif_ocean_waves, Icons.Rounded.Waves),
        NotificationSound("campfire", "Kamp Ateşi", R.raw.notif_campfire, Icons.Rounded.Whatshot),
        NotificationSound("tension", "Gerilim", R.raw.notif_tension, Icons.Rounded.PriorityHigh),
        NotificationSound("magic_chime", "Sihirli Çan", R.raw.notif_magic_chime, Icons.Rounded.AutoAwesome),
        NotificationSound("guitar", "Gitar", R.raw.notif_guitar, Icons.Rounded.Audiotrack),
        NotificationSound("eguitar", "Elektro Gitar", R.raw.notif_eguitar, Icons.Rounded.ElectricBolt),
        NotificationSound("bubble", "Baloncuk", R.raw.notif_bubble, Icons.Rounded.BubbleChart),
        NotificationSound("melody", "Melodi", R.raw.notif_melody, Icons.Rounded.QueueMusic),
        NotificationSound("hell_no", "Hell No 😅", R.raw.notif_hell_no, Icons.Rounded.SentimentVeryDissatisfied),
        NotificationSound("twinkling", "Pırıltı", R.raw.notif_twinkling, Icons.Rounded.Star)
    )

    fun find(id: String): NotificationSound =
        ALL.find { it.id == id } ?: ALL.first { it.id == "bird" }

    fun channelIdFor(id: String): String = "nidado_sound_$id"
}