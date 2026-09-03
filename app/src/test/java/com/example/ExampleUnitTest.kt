package com.example

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun testHebrewDateConversion() {
    // Gregorian: 13 Oct 1993 (Note: Calendar.OCTOBER is 9)
    val cal = Calendar.getInstance()
    cal.set(1993, Calendar.OCTOBER, 13, 12, 0, 0)
    val jd = JewishDate(cal.time)
    println("Jewish year: ${jd.jewishYear}, month: ${jd.jewishMonth}, day: ${jd.jewishDayOfMonth}")
    
    val formatter = HebrewDateFormatter()
    formatter.isHebrewFormat = true
    println("Formatted: ${formatter.format(jd)}")
    
    // Check if month is Tishrei (7) and day is 28
    assertEquals(JewishDate.TISHREI, jd.jewishMonth)
    assertEquals(28, jd.jewishDayOfMonth)
  }
}
