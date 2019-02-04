/*
 * FRC Team 5190
 * Green Hope Falcons
 */

package org.ghrobotics.frc2019

import edu.wpi.first.wpilibj.RobotBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.ghrobotics.frc2019.auto.Autonomous
import org.ghrobotics.frc2019.auto.Trajectories
import org.ghrobotics.frc2019.subsystems.EmergencyHandleable
import org.ghrobotics.frc2019.subsystems.arm.ArmSubsystem
import org.ghrobotics.frc2019.subsystems.drive.DriveSubsystem
import org.ghrobotics.frc2019.subsystems.elevator.ElevatorSubsystem
import org.ghrobotics.frc2019.subsystems.intake.IntakeSubsystem
import org.ghrobotics.lib.commands.FalconSubsystem
import org.ghrobotics.lib.wrappers.FalconRobotBase

object Robot : FalconRobotBase(), CoroutineScope {

    override val coroutineContext = Job()
    val emergencyReadySystems = ArrayList<EmergencyHandleable>()

    var emergencyActive = false

    // Initialize all systems.
    override fun initialize() {
        +DriveSubsystem
        +ElevatorSubsystem
        +ArmSubsystem
        +IntakeSubsystem

        Network
        Autonomous
        Trajectories

//        VisionProcessing
    }

    override fun periodic() {
        Controls.update()
        Network.update()
        Autonomous.update()
        LEDs.update()
/*
        val bestTargetRawData = RawDataTracker.bestTargetRawData
        if (bestTargetRawData != null) {
            DriveSubsystem.localization.addVisionSample(bestTargetRawData, Trajectories.kLoadingStation)
        }*/
    }

    override operator fun FalconSubsystem.unaryPlus() {
        addToSubsystemHandler(this)
        if (this is EmergencyHandleable) {
            emergencyReadySystems.add(this)
        }
    }
}

fun main() {
    RobotBase.startRobot { Robot }
}