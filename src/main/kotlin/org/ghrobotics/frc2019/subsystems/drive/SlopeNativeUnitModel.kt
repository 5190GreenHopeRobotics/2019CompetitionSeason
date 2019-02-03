package org.ghrobotics.frc2019.subsystems.drive

import org.ghrobotics.lib.mathematics.units.Length
import org.ghrobotics.lib.mathematics.units.SIUnit
import org.ghrobotics.lib.mathematics.units.nativeunits.NativeUnit
import org.ghrobotics.lib.mathematics.units.nativeunits.NativeUnitModel

class SlopeNativeUnitModel<T : SIUnit<T>>(
    val modelledSample: T,
    val nativeUnitSample: NativeUnit
) : NativeUnitModel<T>(modelledSample.createNew(0.0)) {

    private val slope = modelledSample.value / nativeUnitSample.value

    override fun fromNativeUnitPosition(nativeUnits: Double) =
        nativeUnits * slope

    override fun toNativeUnitPosition(modelledUnit: Double) =
        modelledUnit / slope

}

fun SlopeNativeUnitModel<Length>.wheelRadius(sensorUnitsPerRotation: NativeUnit) =
    Length(modelledSample.value / (nativeUnitSample.value / sensorUnitsPerRotation.value) / 2.0 / Math.PI)