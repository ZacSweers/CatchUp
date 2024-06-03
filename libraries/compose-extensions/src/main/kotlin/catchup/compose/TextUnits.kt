package catchup.compose

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isUnspecified

operator fun TextUnit.minus(other: TextUnit): TextUnit =
  when {
    isSp && other.isSp -> TextUnit(value - other.value, TextUnitType.Sp)
    isEm && other.isEm -> TextUnit(value - other.value, TextUnitType.Em)
    isUnspecified && other.isUnspecified -> TextUnit(value - other.value, TextUnitType.Unspecified)
    else -> error("Can't subtract different TextUnits. Got $type and ${other.type}")
  }
