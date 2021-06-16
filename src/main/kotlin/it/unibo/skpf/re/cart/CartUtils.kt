package it.unibo.skpf.re.cart

import it.unibo.skpf.re.OriginalValue
import it.unibo.skpf.re.OriginalValue.Interval.*
import it.unibo.skpf.re.utils.getFieldValue
import smile.base.cart.OrdinalNode
import smile.data.type.StructType

typealias LeafConstraints = List<Pair<String, OriginalValue>>
typealias LeafSequence = Sequence<Pair<LeafConstraints, Any>>

val OrdinalNode.value: Double
    get() = getFieldValue("value")

internal fun OrdinalNode.split(fields: StructType) = Pair(
    fields.field(this.feature()).name to LessThan(this.value),
    fields.field(this.feature()).name to GreaterThan(this.value)
)