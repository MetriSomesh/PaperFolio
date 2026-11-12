package com.paperfolio.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Dhan-style LIGHT theme.
//
// Dhan's app is light: white background, near-black text, hairline borders
// instead of elevated dark cards, and one vivid green / one vivid red.
// The token names below are kept from the old dark theme so existing
// screens keep compiling — the VALUES are the Dhan light roles.
// Contrast (white background, AA = 4.5:1 for text):
//   TextPrimary   #12151A  18.5:1   TextSecondary #5B6675  5.8:1
//   TextTertiary  #6B7686  4.6:1    ProfitGreen   #007A5E  5.3:1
//   LossRed       #D92D20  4.8:1    LossRedDark   #C62828  5.6:1
//   BseColor      #7C3AED  5.7:1    EtfColor      #B45309  5.0:1
// ---------------------------------------------------------------------------

// Backgrounds & surfaces (white, hairline borders — no dark cards)
val BackgroundDark = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFFFFFFFF)
val SurfaceElevatedDark = Color(0xFFF2F4F7)
val SurfaceCard = Color(0xFFFFFFFF)
val BorderDark = Color(0xFFE6E8EC)
val BorderStrong = Color(0xFFD8DBE0)

// Profit & Loss
val ProfitGreen = Color(0xFF007A5E)          // green TEXT on white (AA)
val ProfitGreenBg = Color(0xFFE6F4F0)        // pale green tint behind badges
val LossRed = Color(0xFFD92D20)              // red text on white (AA) + SELL fill
val LossRedDark = Color(0xFFC62828)          // red text on the pale red tint
val LossRedBg = Color(0xFFFDECEA)
val LossRedBright = Color(0xFFE5484D)        // red chart line / graphics
val NeutralGray = Color(0xFF8F96A3)

// Brand accents
val BrandPrimary = Color(0xFF007A5E)         // primary actions, selected states
val DhanGreen = Color(0xFF00B386)            // vivid green: chart line, dots
val BuyGreen = Color(0xFF00916E)             // BUY button fill (white text)
val BrandAccent = Color(0xFF2563EB)
val GoldAccent = Color(0xFFFFB800)

// Text
val TextPrimary = Color(0xFF12151A)
val TextSecondary = Color(0xFF5B6675)
val TextTertiary = Color(0xFF6B7686)

// Exchange accents (text on white)
val NseColor = Color(0xFF007A5E)
val BseColor = Color(0xFF7C3AED)
val EtfColor = Color(0xFFB45309)
