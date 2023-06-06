package com.jku.mms_projekt.annotations

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers

@Preview(
    name = "Green",
    showBackground = true,
    wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE,
    showSystemUi = true,
    group = "Green"
)
@Preview(
    name = "Red",
    showBackground = true,
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE,
    showSystemUi = true,
    group = "Red"
)
@Preview(
    name = "Blue",
    showBackground = true,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE,
    showSystemUi = true,
    group = "Blue"
)
@Preview(
    name = "Yellow",
    showBackground = true,
    wallpaper = Wallpapers.YELLOW_DOMINATED_EXAMPLE,
    showSystemUi = true,
    group = "Yellow"
)
@Preview(
    name = "None",
    showBackground = true,
    wallpaper = Wallpapers.NONE,
    showSystemUi = true,
    group = "None"
)
annotation class AllColorPreview()
