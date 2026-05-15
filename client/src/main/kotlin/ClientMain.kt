import java.io.PrintStream
import java.nio.charset.StandardCharsets

fun main() {
    System.setOut(PrintStream(System.out, true, StandardCharsets.UTF_8))
    start {
        up()
    }
}
