package it.unibo.skpf.re

import smile.base.cart.SplitRule
import smile.classification.*
import smile.data.*
import smile.data.formula.Formula
import smile.io.Read
import smile.validation.metric.*

fun Array<*>.toInt() =
    this.map { it.toString().toInt() }.toIntArray()

fun accuracy(data: DataFrame, predictor: Classifier<DoubleArray>): Double {
    return Accuracy.of(data.classesArray(), predictor.predict(data.inputsArray())).round(2)
}

fun accuracy(data: DataFrame, predictor: Extractor<*, *>): Double {
    return Accuracy.of(data.classesArray(), predictor.predict(data).toInt()).round(2)
}

fun accuracy(data: DataFrame, predictor: DecisionTree): Double {
    return Accuracy.of(data.classesArray(), predictor.predict(data)).round(2)
}

fun fidelity(
    data: DataFrame,
    predictor: Classifier<DoubleArray>,
    extractor: Extractor<*, *>
): Double {
    return Accuracy.of(predictor.predict(data.inputsArray()), extractor.predict(data).toInt()).round(2)
}

fun confusionMatrix(data: DataFrame, predictor: Classifier<DoubleArray>,
                    extractor: Extractor<*, *>): ConfusionMatrix {
    return ConfusionMatrix.of(
        predictor.predict(data.inputsArray()),
        extractor.predict(data).toInt().map { if (it == -1) data.nCategories() else it}.toIntArray()
    )
}

fun confusionMatrix(data: DataFrame, predictor: Classifier<DoubleArray>): ConfusionMatrix {
    return ConfusionMatrix.of(
        data.classesArray(),
        predictor.predict(data.inputsArray())
    )
}

fun confusionMatrix(data: DataFrame, predictor: DecisionTree): ConfusionMatrix {
    return ConfusionMatrix.of(
        data.classesArray(),
        predictor.predict(data)
    )
}

fun confusionMatrix(data: DataFrame, extractor: Extractor<*, *>): ConfusionMatrix {
    return ConfusionMatrix.of(
        data.classesArray(),
        extractor.predict(data).toInt().map { if (it == -1) data.nCategories() else it}.toIntArray()
    )
}

fun testClassifier(test: DataFrame, predictor: Classifier<DoubleArray>,
                   printAccuracy: Boolean = true, printMatrix: Boolean = false): Double {
    val accuracy = accuracy(test, predictor)
    if (printAccuracy)
        println("Classifier accuracy: $accuracy")
    if (printMatrix)
        println(confusionMatrix(test, predictor))
    return accuracy
}

fun testClassificationExtractor(name: String, train: DataFrame, test: DataFrame,
                                extractor: Extractor<*, *>,
                                predictor: Classifier<DoubleArray>,
                                printAccuracy: Boolean = true, printMatrix: Boolean = false,
                                printRules: Boolean = false
): ExtractorPerformance {
    val theory = extractor.extract(train)
    val fidelity = fidelity(test, predictor, extractor)
    val accuracy = accuracy(test, extractor)
    val missing = 1.0 * extractor.predict(test).count { it == -1 } / test.nrows()
    if (printAccuracy)
        println(name + " " + theory.size +
                " rules with fidelity of " + fidelity +
                " and accuracy of " + accuracy)
    if (printMatrix)
        println(confusionMatrix(test, predictor, extractor))
    if (printRules)
        theory.clauses.forEach { println(it.toString()) }
    return ExtractorPerformance(fidelity, accuracy, theory.size.toInt(), missing)
}

fun testClassificationExtractor(name: String, train: DataFrame, test: DataFrame,
                                extractor: Extractor<Tuple, DecisionTree>,
                                printAccuracy: Boolean = true, printMatrix: Boolean = false,
                                printRules: Boolean = false
): ExtractorPerformance {
    val theory = extractor.extract(train)
    val accuracy = accuracy(test, extractor)
    if (printAccuracy)
        println(name + " " + theory.size +
                " rules with accuracy of " + accuracy)
    if (printMatrix)
        println(confusionMatrix(test, extractor))
    if (printRules)
        theory.clauses.forEach { println(it.toString()) }
    return ExtractorPerformance(1.0, accuracy, theory.size.toInt(), 0.0)
}

fun classify(name: String, testSplit: Double) {
    println("*** $name ***")
    val dataset = Read.csv("datasets/$name")
    val featureSets = dataset.splitFeatures()
    val (train, test) = dataset.toBoolean(featureSets).randomSplit(testSplit)
    val x = train.inputsArray()
    val y = train.classesArray()
    val knn = knn(x, y, 9)
//        saveToFile("irisKNN9.txt", knn)
//        saveToFile("irisTest50.txt", test)
//        saveToFile("irisTrain50.txt", train)
//        saveToFile("irisBoolFeatSet.txt", featureSets)
    testClassifier(test, knn)
    val real = Extractor.ruleExtractionAsLearning(knn, featureSets)
    testClassificationExtractor("REAL", train, test, real, knn)
    val duepan = Extractor.duepan(knn, featureSets)
    testClassificationExtractor("Duepan", train, test, duepan, knn)
    val cart = cart(
        Formula.lhs("Class"),
        train.inputs().merge(train.classes()),
        SplitRule.GINI,
        20, 0, 5
    )
    val cartEx = Extractor.cart(cart, featureSets)
    testClassificationExtractor("CART", train, test, cartEx)
}