package com.lsing.timego.domain

/** Parses the exact SVG path subset [com.lsing.timego.ui.common.MusclePathSpec.pathData] uses: an
 *  absolute "M x,y" start, followed by relative h/v/l commands (SVG's implicit-repeat rule
 *  applies -- multiple bare coordinate pairs after one command letter keep using that command),
 *  and an optional trailing "z". No curves or other command types appear in the traced source
 *  data, so a full path grammar isn't needed. Returns plain Kotlin (Float, Float) pairs rather
 *  than a Compose Path/Offset so this stays unit-testable without any Android/Compose runtime --
 *  the UI layer turns the result into an actual drawable Path. */
fun parsePathVertices(d: String): List<Pair<Float, Float>> {
    val tokens = Regex("[a-zA-Z]|-?\\d+(?:\\.\\d+)?").findAll(d).map { it.value }.toList()
    val points = mutableListOf<Pair<Float, Float>>()
    var x = 0f
    var y = 0f
    var i = 0
    var cmd = ' '
    while (i < tokens.size) {
        val t = tokens[i]
        if (t.length == 1 && t[0].isLetter()) {
            cmd = t[0]
            i++
            continue
        }
        when (cmd) {
            'M', 'm' -> {
                x = t.toFloat(); y = tokens[i + 1].toFloat(); i += 2
                points.add(x to y)
                // SVG spec: a moveto followed by further bare coordinate pairs treats them as
                // implicit linetos, relative because every moveto here was originally relative
                // ("m") before baking swapped just the first one to absolute for self-containment.
                cmd = 'l'
            }
            'l' -> {
                x += t.toFloat(); y += tokens[i + 1].toFloat(); i += 2
                points.add(x to y)
            }
            'h' -> {
                x += t.toFloat(); i++
                points.add(x to y)
            }
            'v' -> {
                y += t.toFloat(); i++
                points.add(x to y)
            }
            else -> i++
        }
    }
    return points
}

/** Tight bounding box (minX, minY, maxX, maxY) over every point in [vertexLists], expanded by
 *  [padding] on each side -- backs the cropped muscle-group diagram, which draws only a subset of
 *  the full anatomy figure and needs a viewBox scoped to just those shapes instead of the whole
 *  body's. Returns null for empty input (no shapes to crop to). */
fun boundingBox(vertexLists: List<List<Pair<Float, Float>>>, padding: Float = 0f): FloatArray? {
    val allPoints = vertexLists.flatten()
    if (allPoints.isEmpty()) return null
    val minX = allPoints.minOf { it.first } - padding
    val minY = allPoints.minOf { it.second } - padding
    val maxX = allPoints.maxOf { it.first } + padding
    val maxY = allPoints.maxOf { it.second } + padding
    return floatArrayOf(minX, minY, maxX, maxY)
}
