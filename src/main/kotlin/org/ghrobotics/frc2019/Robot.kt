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
import org.ghrobotics.frc2019.subsystems.drive.DriveSubsystem
import org.ghrobotics.frc2019.subsystems.elevator.ElevatorSubsystem
import org.ghrobotics.frc2019.vision.VisionProcessing
import org.ghrobotics.lib.commands.FalconSubsystem
import org.ghrobotics.lib.wrappers.FalconRobotBase

object Robot : FalconRobotBase(), CoroutineScope {

    override val coroutineContext = Job()
    val emergencyReadySystems = ArrayList<EmergencyHandleable>()

    // Initialize all systems.
    override fun initialize() {
        +DriveSubsystem
        +ElevatorSubsystem

        Network
        Autonomous
        Trajectories

        VisionProcessing
    }

    override fun periodic() {
        Controls.update()
        Network.update()
        Autonomous.update()
    }

    override operator fun FalconSubsystem.unaryPlus() {
        addToSubsystemHandler(this)
        if (this is EmergencyHandleable) {
            emergencyReadySystems.add(this)
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        RobotBase.startRobot { Robot }
    }
}