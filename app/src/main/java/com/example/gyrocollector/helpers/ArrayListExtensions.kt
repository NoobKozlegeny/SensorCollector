package com.example.gyrocollector.helpers

fun ArrayList<String>.convertEmptyDataToDefault(): ArrayList<String> {
    for (i in this.indices) {
        if (this[i] == "") {
            this[i] = "0,0,0"
        }
    }
    return this
}

fun ArrayList<String>.equalizeSensorList(
    maxLength: Int,
    lineToAdd: String
): ArrayList<String> {
    var i = this.size
    while (i < maxLength) {
        this.add(lineToAdd)
        i++
    }
    return this
}