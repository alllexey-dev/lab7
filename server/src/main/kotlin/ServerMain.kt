import commands.inner.ExitSignal

fun main(args: Array<String>) {
    serverContainer(args = args) {
        up()
    }
}
