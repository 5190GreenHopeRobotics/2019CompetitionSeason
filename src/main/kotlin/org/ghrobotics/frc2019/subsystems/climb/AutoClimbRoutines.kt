package org.ghrobotics.frc2019.subsystems.climb

import org.ghrobotics.frc2019.Constants
import org.ghrobotics.frc2019.subsystems.arm.ClosedLoopArmCommand
import org.ghrobotics.frc2019.subsystems.drive.DriveSubsystem
import org.ghrobotics.lib.commands.*
import org.ghrobotics.lib.mathematics.units.degree
import org.ghrobotics.lib.mathematics.units.second
import org.ghrobotics.lib.utils.Source

fun autoL3Climb() = sequential {
    // Climb up
    +ClosedLoopClimbCommand(Constants.kClimbFrontL3Ticks, Constants.kClimbBackL3Ticks)
    +parallel {
        val group = sequential {
            // Go forward in till it goes onto platform
            +parallel {
                +ClimbWheelCommand(Source(1.0))
                +ClosedLoopArmCommand(180.degree)
            }.withExit { ClimbSubsystem.lidarRaw < 500 }
            // Raise back winch
            +parallel {
                +ClosedLoopArmCommand(135.degree)
                +ClimbWheelCommand(Source(0.2))
                +ResetWinchCommand(ClimbSubsystem.Winch.BACK)
            }.withExit { ClimbSubsystem.Winch.BACK.motor.sensorCollection.isRevLimitSwitchClosed }
            // Yote forward
            +ClimbWheelCommand(Source(1.0)).withTimeout(1.5.second)
            +ResetWinchCommand(ClimbSubsystem.Winch.FRONT)
            +DelayCommand(1.second)
        }
        +object : FalconCommand(DriveSubsystem) {
            init {
                finishCondition += group.wrappedValue::isCompleted
            }

            override suspend fun execute() {
                DriveSubsystem.tankDrive(-.4, -.4)
            }
        }
        +group
    }
}