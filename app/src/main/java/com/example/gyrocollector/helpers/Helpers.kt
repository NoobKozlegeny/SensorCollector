@file:JvmName("Helpers")

package com.example.gyrocollector.helpers

import android.content.Intent
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList


//Saves data to CSV file
fun createIntent(name: String, selectedMode: String): Intent {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "text/csv"
    val fileName = selectedMode + name + LocalDate.now().toString().split("-".toRegex())
        .dropLastWhile { it.isEmpty() }
        .toTypedArray()[1] +LocalDate.now().toString().split("-".toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray()[2] + ".csv"
    intent.putExtra(Intent.EXTRA_TITLE, fileName)

    return intent;
}

fun equalizeList(list: ArrayList<String>, lineToAdd: String, maxLength: Int): ArrayList<String> {
    var i: Int = list.size
    while (i < maxLength) {
        list.add(lineToAdd)
        i++
    }

    return list;
}

