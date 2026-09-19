package com.example.bodycamoutdoorgear.sound

import com.example.bodycamoutdoorgear.R

data class ShutterSound(
    val id: String,
    val emoji: String,
    val name: String,
    val rawRes: Int? = null,
    val isCustom: Boolean = false
) {
    companion object {
        val CLASSIC = ShutterSound("classic", "📷", "Classic beep")
        val DUCK = ShutterSound("duck", "🦆", "Duck quack", R.raw.shutter_duck)
        val CAT = ShutterSound("cat", "🐱", "Cat meow", R.raw.shutter_cat)
        val DOG = ShutterSound("dog", "🐶", "Dog bark", R.raw.shutter_dog)

        val builtIn = listOf(CLASSIC, DUCK, CAT, DOG)

        fun byId(id: String?): ShutterSound = builtIn.firstOrNull { it.id == id } ?: CLASSIC
    }
}
