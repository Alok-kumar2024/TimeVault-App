package com.example.timevault.Model

private var lastTime = 0L
private var counter = 0L

@Synchronized
private fun generateUniqueID(prefix : String) : String
{
    val now = System.currentTimeMillis()

    if (now != lastTime)
    {
        lastTime =  now
        counter = 0
    }else{
        counter++
    }

    val timepart = now.toString(36)
    val counterPart = counter.toString(36).padStart(3,'0')

    return "${prefix}_${timepart}_${counterPart}"
}

fun useGenerateID (value : String) : String
{
    return generateUniqueID(value)
}