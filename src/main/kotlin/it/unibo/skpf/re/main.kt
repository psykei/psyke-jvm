package it.unibo.skpf.re

import smile.classification.*
import smile.data.*
import smile.io.Read

fun main() {

    val tests = listOf(Pair("iris", 0.5), Pair("car", 0.2))

    for ((name, testSplit) in tests) {
        println("*** $name ***")
        val dataset = Read.csv("datasets/$name.data")
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
        testClassificationExtractor("REAL", train, test, real, knn, printRules = true)
        val duepan = Extractor.duepan(knn, featureSets)
        testClassificationExtractor("Duepan", train, test, duepan, knn, printRules = true)
        println()
    }
}
