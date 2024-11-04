import org.example.FAAQueue
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.jetbrains.kotlinx.lincheck.verifier.VerifierState
import kotlin.test.Test

class FAATest {
    private val q = FAAQueue<Int>()

    @Operation
    fun enqueue(x: Int): Unit = q.enqueue(x)

    @Operation
    fun dequeue(): Int? = q.dequeue()

    fun isEmpty(): Boolean = q.isEmpty

    @Test
    fun modelCheckingTest() =  ModelCheckingOptions()
            .iterations(100)
            .invocationsPerIteration(10_000)
            .threads(3)
            .actorsPerThread(3)
            .checkObstructionFreedom()
            .sequentialSpecification(IntQueueSequential::class.java)
            .check(this::class.java)

    @Test
    fun stressTest() = StressOptions()
            .iterations(100)
            .invocationsPerIteration(50_000)
            .threads(3)
            .actorsPerThread(3)
            .sequentialSpecification(IntQueueSequential::class.java)
            .check(this::class.java)
}

class IntQueueSequential : VerifierState() {
    private val q = ArrayDeque<Int>()

    fun enqueue(x: Int) {
        q.addLast(x)
    }

    fun dequeue(): Int? = q.removeFirstOrNull()

    fun isEmpty(): Boolean = q.isEmpty()

    override fun extractState() = q
}