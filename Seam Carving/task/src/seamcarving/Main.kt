package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt


//fun createImage(width: Int, height: Int): BufferedImage {
//    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
//    val g = bufferedImage.graphics
//    g.color = Color.BLACK
//    g.fillRect(0, 0, width, height)
//    g.color = Color.RED
//    g.drawLine(0, 0, width - 1, height - 1)
//    g.drawLine(0, height - 1, width - 1, 0)
//    return bufferedImage
//}
//
//fun main() {
//    println("Enter rectangle width:")
//    val width = readLine()!!.toInt()
//    println("Enter rectangle height:")
//    val height = readLine()!!.toInt()
//    println("Enter output image name:")
//    val path = readLine()!!
//
//    val img = createImage(width, height)
//    val output = File(path)
//    ImageIO.write(img, "png", output)
//}

fun negateImage(img: BufferedImage): BufferedImage {
    val width = img.width
    val height = img.height
    for (y in 0 until height) {
        for (x in 0 until width) {
            var pixel = img.getRGB(x, y)
            val a = pixel.shr(24).and(0xff)
            val r = 255 - pixel.shr(16).and(0xff)
            val g = 255 - pixel.shr(8).and(0xff)
            val b = 255 - pixel.and(0xff)
            pixel = a.shl(24).or(r.shl(16).or(g.shl(8).or(b)))
            img.setRGB(x, y, pixel)
        }
    }
    return img
}

fun energy_calc(xminus: Color, xplus: Color, yminus: Color, yplus: Color): Double {
    val x_diffR = (xminus.red - xplus.red).toDouble()
    val x_diffG = (xminus.green - xplus.green).toDouble()
    val x_diffB = (xminus.blue - xplus.blue).toDouble()
    val x_gradient = x_diffR.pow(2.0) + x_diffG.pow(2.0) + x_diffB.pow(2.0)
    val y_diffR = (yminus.red - yplus.red).toDouble()
    val y_diffG = (yminus.green - yplus.green).toDouble()
    val y_diffB = (yminus.blue - yplus.blue).toDouble()
    val y_gradient = y_diffR.pow(2.0) + y_diffG.pow(2.0) + y_diffB.pow(2.0)
    return sqrt(x_gradient + y_gradient)
}

fun calculateEnergy(image: BufferedImage): Pair<Array<Array<Double>>, Double> {
    var xIndex: Int
    var yIndex: Int
    val energy: Array<Array<Double>> = Array(image.width) { Array(image.height) {0.0} }
    var maxEnergy = 0.0
    for (i in 0 until image.width) {
        xIndex = when (i) {
            0 -> 1
            image.width - 1 -> image.width - 2
            else -> i
        }
        for (j in 0 until image.height) {
            yIndex = when (j) {
                0 -> 1
                image.height - 1 -> image.height - 2
                else -> j
            }
            val xminus = Color(image.getRGB(xIndex - 1, j))
            val xplus = Color(image.getRGB(xIndex + 1, j))
            val yminus = Color(image.getRGB(i, yIndex - 1))
            val yplus = Color(image.getRGB(i, yIndex + 1))
            energy[i][j] = energy_calc(xminus, xplus, yminus, yplus)
            if (maxEnergy < energy[i][j]) maxEnergy = energy[i][j]
        }
    }
    return energy to maxEnergy
}

fun processImage(energy: Pair<Array<Array<Double>>, Double>): BufferedImage {
    val image = BufferedImage(energy.first.size, energy.first[0].size, BufferedImage.TYPE_INT_RGB)
    for (i in 0 until image.width) {
        for (j in 0 until image.height) {
            val intensity = (255 * energy.first[i][j] / energy.second).toInt()
            val colorNew = Color(intensity , intensity, intensity)
            image.setRGB(i, j, colorNew.rgb)
        }
    }
    return image
}

fun min(a: Double, b: Double, c: Double): Double {
    var m = a
    if (m > b) m = b
    if (m > c) m = c
    return m
}

fun findSeamVertical(energy: Array<Array<Double>>): Array<Array<Double>> {
    val matrix = energy
    val height = matrix[0].indices
    val width = matrix.indices
    for (j in width)
        matrix[j][0] = energy[j][0]
    for (i in 1 until matrix[0].size) {
        for (j in width) {
            val minUpstream = when (j) {
                0 -> kotlin.math.min(matrix[0][i - 1], matrix[1][i - 1])
                width.last -> kotlin.math.min(matrix[width.last][i - 1], matrix[width.last - 1][i - 1])
                else -> min(matrix[j - 1][i - 1], matrix[j][i - 1], matrix[j + 1][i - 1])
            }
            matrix[j][i] += minUpstream
        }
    }
    return matrix
}

fun findSeamHorizontal(energy: Array<Array<Double>>): Array<Array<Double>> {
    val matrix = energy
    val height = matrix[0].indices
    val width = matrix.indices
    for (j in height)
        matrix[0][j] = energy[0][j]
    for (i in 1 until matrix.size) {
        for (j in height) {
            val minUpstream = when (j) {
                0 -> kotlin.math.min(matrix[i - 1][0], matrix[i - 1][0])
                height.last -> kotlin.math.min(matrix[i - 1][height.last], matrix[i - 1][height.last - 1])
                else -> min(matrix[i - 1][j - 1], matrix[i - 1][j], matrix[i - 1][j + 1])
            }
            matrix[i][j] += minUpstream
        }
    }
    return matrix
}

fun getSeamVertical(matrix: Array<Array<Double>>): Array<Int> {
    val result = Array (matrix[0].size) { 0 }
    val lastline = matrix[0].size - 1
    var minpos = 0
    for (i in matrix.indices) {
        if (matrix[i][lastline] < matrix[minpos][lastline]) {
            minpos = i
            result[lastline] = i
        }
    }
    for (y in lastline - 1 downTo 0) {
        val prevX = result[y + 1]
        result[y] = prevX
        if (prevX > 0 && matrix[prevX][y] > matrix[prevX - 1][y]) result[y] = prevX - 1
        if (prevX < matrix.size - 1 && matrix[result[y]][y] > matrix[prevX + 1][y]) result[y] = prevX + 1
    }
    return result
}

fun getSeamHorizontal(matrix: Array<Array<Double>>): Array<Int> {
    val result = Array (matrix.size) { 0 }
    val lastline = matrix.size - 1
    var minpos = 0
    for (i in matrix[0].indices) {
        if (matrix[lastline][i] < matrix[lastline][minpos]) {
            minpos = i
            result[lastline] = i
        }
    }
    for (x in lastline - 1 downTo 0) {
        val prevY = result[x + 1]
        result[x] = prevY
        if (prevY > 0 && matrix[x][prevY] > matrix[x][prevY - 1]) result[x] = prevY - 1
        if (prevY < matrix[0].size - 1 && matrix[x][result[x]] > matrix[x][prevY + 1]) result[x] = prevY + 1
    }
    return result
}

fun main(args: Array<String>) {
//    val inputFilePath = "C:\\Users\\admin\\IdeaProjects\\Seam Carving\\Seam Carving\\task\\blue.png"
//    val outputFilePath = "C:\\Users\\admin\\IdeaProjects\\Seam Carving\\Seam Carving\\task\\bluered.png"
    val inputFilePath = args[1]
    val outputFilePath = args[3]
    val reduceWidth = args[5].toInt()
    val reduceHeight = args[7].toInt()
//    val reduceWidth = 125
//    val reduceHeight = 50
    val image: BufferedImage = ImageIO.read(File(inputFilePath))
    var oldImage: BufferedImage = image
    for (i in 0 until reduceWidth) {
        val energy = calculateEnergy(oldImage)
        val matrix = findSeamVertical(energy.first)
        val seam = getSeamVertical(matrix)
        val newImage = BufferedImage(oldImage.width - 1, oldImage.height, BufferedImage.TYPE_INT_RGB)
        for (i in 0 until oldImage.width) {
            for (j in 0 until oldImage.height) {
                if (seam[j] < i) {
                    newImage.setRGB(i - 1, j, oldImage.getRGB(i, j))
                } else if (seam[j] > i) {
                    newImage.setRGB(i, j, oldImage.getRGB(i, j))
                }
            }
        }
        oldImage = newImage
    }
    for (i in 0 until reduceHeight) {
        val energy = calculateEnergy(oldImage)
        val matrix = findSeamHorizontal(energy.first)
        val seam = getSeamHorizontal(matrix)
        val newImage = BufferedImage(oldImage.width, oldImage.height - 1, BufferedImage.TYPE_INT_RGB)
        for (i in 0 until oldImage.width) {
            for (j in 0 until oldImage.height) {
                if (seam[i] < j) {
                    newImage.setRGB(i, j - 1, oldImage.getRGB(i, j))
                } else if (seam[i] > j) {
                    newImage.setRGB(i, j, oldImage.getRGB(i, j))
                }
            }
        }
        oldImage = newImage
    }
//    seam.forEachIndexed { index, i -> image.setRGB(index, i, red.rgb) }
    ImageIO.write(oldImage, "png", File(outputFilePath))
}

