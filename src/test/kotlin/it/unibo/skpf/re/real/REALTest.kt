package it.unibo.skpf.re.real

import it.unibo.skpf.re.BooleanFeatureSet
import it.unibo.skpf.re.Extractor
import it.unibo.skpf.re.utils.createHead
import it.unibo.skpf.re.utils.loadFromFile
import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.core.List
import it.unibo.tuprolog.theory.MutableTheory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import smile.classification.KNN
import smile.data.DataFrame
import smile.data.Tuple
import kotlin.streams.toList

internal class REALTest {

    @Suppress("UNCHECKED_CAST")
    private val knn = loadFromFile("irisKNN9.txt") as KNN<DoubleArray>
    @Suppress("UNCHECKED_CAST")
    private val featureSets = loadFromFile("irisBoolFeatSet.txt") as Set<BooleanFeatureSet>
    private val real = Extractor.ruleExtractionAsLearning(knn, featureSets)
    private val train = loadFromFile("irisTrain50.txt") as DataFrame
    private val theory = real.extract(train)

    @Test
    fun testExtract() {
        val variables = listOf("V1", "V2", "V3", "V4").map { Var.of(it) }
        val cond1 = Struct.of("in", variables[0], List.of(Real.of(5.39), Real.of(6.26)))
        val cond2 = Struct.of("in", variables[2], List.of(Real.of(2.28), Real.of(4.87)))
        val cond3 = Struct.of("in", variables[3], List.of(Real.of(0.65), Real.of(1.64)))

        val expectedTheory = MutableTheory.of(
            Clause.of(
                createHead("iris", variables, "setosa"),
                Struct.of("=<", variables[3], Real.of(0.65))
            ),
            Clause.of(
                createHead("iris", variables, "versicolor"),
                cond2, cond3
            ),
            Clause.of(
                createHead("iris", variables, "versicolor"),
                cond1, cond3
            ),
            Clause.of(
                createHead("iris", variables, "versicolor"),
                cond1, cond2
            ),
            Clause.of(
                createHead("iris", variables, "virginica"),
                Struct.of(">", variables[2], Real.of(4.87))
            ),
            Clause.of(
                createHead("iris", variables, "virginica"),
                Struct.of("in", variables[1], List.of(Real.of(2.87), Real.of(3.2))),
                Struct.of(">", variables[3], Real.of(1.64))
            )
        )
        assertEquals(expectedTheory.size, theory.size)
        expectedTheory.zip(theory) { expected, actual ->
            assertTrue(expected.structurallyEquals(actual))
        }
    }

    @Test
    fun testPredict() {
        fun Tuple.check(field: String): Boolean =
            this[field].toString().toDouble() == 1.0

        val test = loadFromFile("irisTest50.txt") as DataFrame
        val predictions = real.predict(test)
        val expected = sequence {
            for (sample in test.stream().toList())
                if (sample.check("PetalWidth_0"))
                    yield(0)
                else if (
                    sample.check("PetalLength_1") && sample.check("PetalWidth_1") ||
                    sample.check("SepalLength_1") && sample.check("PetalWidth_1") ||
                    sample.check("PetalLength_1") && sample.check("SepalLength_1")
                )
                    yield(1)
                else if (sample.check("PetalLength_2") ||
                    sample.check("SepalWidth_1") && sample.check("PetalWidth_2")
                )
                    yield(2)
                else
                    yield(-1)
        }
        assertEquals(expected.toList(), predictions.toList())
    }
}