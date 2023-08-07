package dev.annachiara.randompicks

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.annachiara.randompicks.ui.theme.RandomPicksTheme
import kotlin.math.sign
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bitmap = makeCarpet()

        setContent {
            RandomPicksTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RandomCarpet(bitmap)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
    }
}

data class Block(var stripeWidth: Int = 3, var offset: Int = 2,
                 val blockWidth: Int = 8,
                 val minStripeWidth: Int = 2,
                 val maxStripeWidth: Int = 5,
                 val preferredStripeWidth: Int = 2
                 ) {
    val changeWidthChance = .15
    val moveChance = .4

    fun next(): Block {
        val b = this.copy()

        val r = Random.nextDouble()
        if (r < changeWidthChance) {
            b.newWidth()
        } else if (r < moveChance) {
            b.newOffset()
        }
        return b
    }

    fun newOffset() {
        if (offset == 0) {
            offset = 1
        } else if (offset == blockWidth-stripeWidth) {
            offset -=1
        } else {
            offset += ((Random.nextDouble()-.5).sign).toInt()
        }
    }

    fun newWidth() {
        val currentWidth = stripeWidth
        // If we are at the minimum width, increase
        if (stripeWidth == minStripeWidth) stripeWidth++
        // If we are at the maximum width, decrease
        else if (stripeWidth == maxStripeWidth) stripeWidth--
        // If we are at the preferred width, randomly up or down
        else if (stripeWidth == preferredStripeWidth)
            stripeWidth += ((Random.nextDouble() -.5).sign).toInt()
        // Else
        else {
            // Are we above or below the preferred width?
            val dir = (preferredStripeWidth - stripeWidth).sign
            // Go towards the preferred width with 75% probability, away from it with 25% chance
            stripeWidth += dir * ((Random.nextDouble() - .25).sign).toInt()
        }
        // Adjust the offset
        // If we stretched over the end, just step back
        if (offset > blockWidth - stripeWidth) {
            offset = blockWidth - stripeWidth
        } else if (offset > 0 && offset < blockWidth - stripeWidth) {
            val goRight = Random.nextDouble() > .5
            val isWider = stripeWidth > currentWidth
            if ((isWider && !goRight)) {
                offset -=1
            } else if (!isWider && goRight) {
                offset += 1
            }
        }
        // Do nothing if offset is 0
    }

    fun picks(): List<Int> {
        val r = MutableList(blockWidth) {0}
        for (i in offset until(offset+stripeWidth)) {
            r[i] = 1
        }
        return r
    }
}


fun makeCarpet(): Bitmap {
    var block1 = Block()
    var block2 = Block()
    val threading =
        listOf(
            1, 1, 1, 1, 2, 1, 2, 1, 2, 1, 2,
            1, 1, 2, 2, 1, 1, 2, 2, 1, 1, 2, 2,
            1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1
        )

    val threadCount = threading.size*8
    val pickCount = threadCount
    val threadWidth = 4
    val pickWidth = 4


    val bitmap = Bitmap.createBitmap(
        threadCount*threadWidth, pickCount*pickWidth, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    val colors = listOf(Color(230, 217, 138), Color(99, 99, 92))
    val paint = Paint()

    for (row in 0 until pickCount) {
        block1 = block1.next()
        block2 = block2.next()
        for (blockCount in 0 until threading.size) {
            val block = if (threading[blockCount] == 1) block1 else block2
            val interlacing = block.picks()
            for (thread in 0 until block.blockWidth) {
                val y = (row) * pickWidth
                val x = (thread + blockCount*block.blockWidth)*threadWidth
                val w = threadWidth
                val h = pickWidth
                paint.color = colors[interlacing[thread]].toArgb()
                canvas.drawRect(x.toFloat(), y.toFloat(), (x+w).toFloat(), (y+h).toFloat(), paint)
            }
        }
    }
    return bitmap
}


@Composable
fun RandomCarpet(bitmap: Bitmap) {
    var shuttle by rememberSaveable { mutableStateOf(1) }
    var pickNumber by rememberSaveable { mutableStateOf(1)}
    var block1 by remember { mutableStateOf(Block()) }
    var block2 by remember { mutableStateOf(Block()) }
    val threading = remember {
        listOf(
            1, 1, 1, 1, 2, 1, 2, 1, 2, 1, 2,
//            1, 1, 2, 2, 1, 1, 2, 2, 1, 1, 2, 2,
//            1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1
        )
    }


    val color = Color(230, 217, 138)
    val overshotColor = Color(99, 99, 92)
    Row(modifier = Modifier
        .fillMaxSize()
        .background(color),
        verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
            , horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.padding(top=8.dp)) {
                for (handle in 0 until 8) {
                    Box(modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .border(2.dp, overshotColor)) {
                        if (block1.picks()[handle] == 1) {
                            Text("${handle + 1}", modifier = Modifier.align(Alignment.Center),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Row() {
                for (handle in 0 until 8) {
                    Box(modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .border(2.dp, overshotColor)) {
                        if (block2.picks()[handle] == 1) {
                            Text("${handle + 9}", modifier = Modifier.align(Alignment.Center),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                                )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.padding(all = 20.dp))
            Row {
                Box(modifier = Modifier.fillMaxWidth(.1f))
                Button(modifier = Modifier.fillMaxWidth(.6f),
                    onClick = {
                    block1 = block1.next()
                    block2 = block2.next()
                    pickNumber += 1
                }) {
                    Text(
                        "Next", fontSize = 48.sp,
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp)

                    )
                }
                Row(modifier = Modifier.height(80.dp)
                    .padding(start = 24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                    Text("${pickNumber}",
                        fontSize = 24.sp)
                }
            }
            Row {
                Text("O1", fontSize = 24.sp,modifier = Modifier.padding(8.dp))
                Button(onClick = {
                    if (block1.offset > 0) block1 = block1.copy(offset = block1.offset-1)
                },modifier = Modifier.padding(8.dp)) {
                    Text("-")
                }
                Button(onClick = {
                    if (block1.offset < block1.blockWidth - block1.stripeWidth) block1 = block1.copy(offset = block1.offset+1)
                },modifier = Modifier.padding(8.dp)) {
                    Text("+")
                }
                Text("W1", fontSize = 24.sp,modifier = Modifier.padding(8.dp) )
                Button(onClick = {
                    if (block1.stripeWidth > block1.minStripeWidth) block1 = block1.copy(stripeWidth = block1.stripeWidth-1)
                }
                    ,modifier = Modifier.padding(8.dp)) {
                    Text("-")
                }
                Button(onClick = {
                    if (block1.stripeWidth < block1.maxStripeWidth) block1 = block1.copy(stripeWidth = block1.stripeWidth+1)
                },modifier = Modifier.padding(8.dp)) {
                    Text("+")
                }
            }
            Row {
                Text("O2", fontSize = 24.sp,modifier = Modifier.padding(8.dp))
                Button(onClick = {
                    if (block2.offset > 0) block2 = block2.copy(offset = block2.offset-1)
                },modifier = Modifier.padding(8.dp)) {
                    Text("-")
                }
                Button(onClick = {
                    if (block2.offset < block2.blockWidth - block2.stripeWidth) block2 = block2.copy(offset = block2.offset+1)
                },modifier = Modifier.padding(8.dp)) {
                    Text("+")
                }
                Text("W2", fontSize = 24.sp,modifier = Modifier.padding(8.dp))
                Button(onClick = {
                    if (block2.stripeWidth > block2.minStripeWidth) block2 = block2.copy(stripeWidth = block2.stripeWidth-1)
                },modifier = Modifier.padding(8.dp)) {
                    Text("-")
                }
                Button(onClick = {
                    if (block2.stripeWidth < block2.maxStripeWidth) block2 = block2.copy(stripeWidth = block2.stripeWidth+1)
                },modifier = Modifier.padding(8.dp)) {
                    Text("+")
                }
            }
            Image(bitmap.asImageBitmap(), contentDescription = "My chart", modifier = Modifier.fillMaxSize())
        }
    }

}

@Composable
fun RandomPick() {
    val options = remember{ listOf<Int>(2, 2, 2, 2, 4, 4, 4, 4, 6, 6, 8)}
    var currentNumber by rememberSaveable { mutableStateOf(2)}
    var shuttle by rememberSaveable { mutableStateOf(1)};
    val color: Color by animateColorAsState(
        if (shuttle == 1)
            Color(240, 180, 91) else
                Color(255, 248, 224))

    Row(modifier = Modifier
        .fillMaxSize()
        .background(color),
        verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                , horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                style = MaterialTheme.typography.titleLarge,
                fontSize = 64.sp,
                text = "$currentNumber"
            )
            Spacer(modifier = Modifier.padding(all = 20.dp))
            Button(onClick = {
                val i = (Random.nextDouble() * options.size).toInt()
                currentNumber = options[i]
                shuttle = (shuttle) % 2 + 1
            }) {
                Text(
                    "Shuttle $shuttle", fontSize = 48.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                )
            }
        }
    }
}
//
//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    RandomPicksTheme {
//        RandomPick()
//    }
//}