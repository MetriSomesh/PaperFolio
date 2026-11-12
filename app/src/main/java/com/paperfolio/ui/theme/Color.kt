package com.paperfolio.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// VELTO-style theme.
//
// Grounded in the real VELTO fintech concept (New Minds, Awwwards nominee):
// the APP screens are off-white with white cards, one dark-green hero
// card, and one loud lime accent. The deep green is the marketing page;
// inside the app it appears only as the hero card, the active nav pill,
// and selected states. Sampled from the Awwwards submission screenshot:
//   page green  #122D28   lime card  #DCF97B   phone bg   #F4F6F1
// Contrast (AA = 4.5:1 for text, computed — see README):
//   on #F4F6F1: TextPrimary #12151A 16.8:1, TextSecondary #5B6675 5.4:1,
//     TextTertiary #66707E 4.6:1, VeltoGreen #122D28 13.5:1,
//     ProfitGreen #007A5E 4.9:1, LossRed #C62828 5.2:1,
//     BseColor #7C3AED 5.7:1, EtfColor #B45309 5.0:1
//   on lime #DCF97B: VeltoGreen text 12.5:1 (white on lime is 1.2:1 —
//     never put white text on lime; lime always takes dark text)
//   on hero #122D28: HeroText #F4F6F1 13.5:1, HeroTextSecondary #A8B8B0
//     7.1:1, HeroProfit #8FE3B0 9.6:1, HeroLoss #FF9A8A 7.2:1,
//     white 14.7:1
// ---------------------------------------------------------------------------

// Backgrounds & surfaces (off-white app, white cards, soft borders)
val AppBackground = Color(0xFFF4F6F1)
val Surface = Color(0xFFFFFFFF)
val SurfaceMuted = Color(0xFFE9ECE5)
val Border = Color(0xFFE3E6DF)
val BorderStrong = Color(0xFFD3D8CC)

// Dark green — hero card, active nav pill, selected states, focus rings
val VeltoGreen = Color(0xFF122D28)
val VeltoGreenElevated = Color(0xFF1E3833) // inner pills on the hero card

// Text on the dark hero card
val HeroText = Color(0xFFF4F6F1)
val HeroTextSecondary = Color(0xFFA8B8B0)
val HeroProfit = Color(0xFF8FE3B0)
val HeroLoss = Color(0xFFFF9A8A)

// Lime — primary CTA fill only, always with dark (VeltoGreen) text
val Lime = Color(0xFFDCF97B)

// Profit & Loss (text on light backgrounds)
val ProfitGreen = Color(0xFF007A5E)
val ProfitGreenBg = Color(0xFFE6F4F0)        // pale green tint behind badges
val LossRed = Color(0xFFC62828)              // red text on light + SELL fill
val LossRedBg = Color(0xFFFDECEA)
val LossRedBright = Color(0xFFE5484D)        // red chart line / graphics
val NeutralGray = Color(0xFF8F96A3)

// Chart
val ChartGreen = Color(0xFF00B386)           // vivid green chart line / dots

// Text
val TextPrimary = Color(0xFF12151A)
val TextSecondary = Color(0xFF5B6675)
val TextTertiary = Color(0xFF66707E)

// Exchange accents (text on light)
val NseColor = Color(0xFF007A5E)
val BseColor = Color(0xFF7C3AED)
val EtfColor = Color(0xFFB45309)
