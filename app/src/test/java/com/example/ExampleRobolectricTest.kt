package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.KurdishNaturalCommandEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BASOKA", appName)
  }

  @Test
  fun `test kurdish natural command parsing`() {
    val identityCmd = KurdishNaturalCommandEngine.parseKurdishCommand("تۆ کێیت؟")
    assertNotNull(identityCmd)
    assertEquals("assistant_identity", identityCmd?.intent)
    assertEquals("basoka", identityCmd?.target)

    val wifiCmd = KurdishNaturalCommandEngine.parseKurdishCommand("Wi-Fi بکەرەوە")
    assertNotNull(wifiCmd)
    assertEquals("open_settings", wifiCmd?.intent)
    assertEquals("wifi", wifiCmd?.target)

    val mapsCmd = KurdishNaturalCommandEngine.parseKurdishCommand("Google Maps بکەرەوە بۆ هەولێر")
    assertNotNull(mapsCmd)
    assertEquals("open_app", mapsCmd?.intent)
    assertEquals("google_maps", mapsCmd?.target)

    val musicCmd = KurdishNaturalCommandEngine.parseKurdishCommand("مۆسیقا پەخش بکە")
    assertNotNull(musicCmd)
    assertEquals("media_play", musicCmd?.intent)

    val brightnessCmd = KurdishNaturalCommandEngine.parseKurdishCommand("کەمێک ڕووناکی شاشە زیاد بکە")
    assertNotNull(brightnessCmd)
    assertEquals("change_brightness", brightnessCmd?.intent)
  }

  @Test
  fun `test place location and recognized person data classes`() {
    val place = com.example.model.PlaceLocation(
        placeName = "قەڵای هەولێر",
        cityAndCountry = "هەولێر، هەرێمی کوردستان",
        coordinates = "36.1912° N, 44.0091° E",
        mapsQuery = "Erbil Citadel"
    )
    assertEquals("قەڵای هەولێر", place.placeName)
    assertEquals("36.1912° N, 44.0091° E", place.coordinates)

    val person = com.example.model.RecognizedPerson(
        fullName = "مام جەلال تاڵەبانی",
        profession = "سەرۆک کۆمار و سیاسەتمەدار",
        accounts = listOf(
            com.example.model.SocialAccount("Wikipedia", "Jalal Talabani")
        )
    )
    assertEquals("مام جەلال تاڵەبانی", person.fullName)
    assertEquals(1, person.accounts.size)
  }
}

