/*
 * FRC Team 5190
 * Green Hope Falcons
 */

package org.ghrobotics.frc2019

import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab
import org.ghrobotics.frc2019.auto.AutoMode
import org.ghrobotics.frc2019.auto.StartingPositions
import org.ghrobotics.frc2019.subsystems.drive.DriveSubsystem
import org.ghrobotics.frc2019.vision.TargetTracker
import org.ghrobotics.lib.wrappers.networktables.enumSendableChooser

object Network {

    val startingPositionChooser = enumSendableChooser<StartingPositions>()
    val autoModeChooser = enumSendableChooser<AutoMode>()

    private val mainShuffleboardDisplay: ShuffleboardTab = Shuffleboard.getTab("5190")

    private val autoLayout = mainShuffleboardDisplay.getLayout("Autonomous", BuiltInLayouts.kList)
        .withSize(2, 2)
        .withPosition(0, 0)

    private val localizationLayout = mainShuffleboardDisplay.getLayout("Localization", BuiltInLayouts.kList)
        .withSize(2, 2)
        .withPosition(2, 0)

    private val visionLayout = mainShuffleboardDisplay.getLayout("Vision", BuiltInLayouts.kGrid)
        .withSize(3, 3)
        .withPosition(0, 2)

    private val driveSubsystemLayout = mainShuffleboardDisplay.getLayout("Drive", BuiltInLayouts.kGrid)
        .withSize(2, 2)
        .withPosition(4, 0)

    private val globalXEntry = localizationLayout.add("Robot X", 0.0).entry
    private val globalYEntry = localizationLayout.add("Robot Y", 0.0).entry
    private val globalAEntry = localizationLayout.add("Robot Angle", 0.0).entry

    private val leftPositionEntry = driveSubsystemLayout.add("Left Encoder", 0.0).entry
    private val rightPositionEntry = driveSubsystemLayout.add("Right Encoder", 0.0).entry
    private val leftAmperageEntry = driveSubsystemLayout.add("Left Current", 0.0).entry
    private val rightAmperageEntry = driveSubsystemLayout.add("Right Current", 0.0).entry

    private val visionTargetX = visionLayout.add("Vision Target X", 0.0).entry
    private val visionTargetY = visionLayout.add("Vision Target Y", 0.0).entry
    private val visionTargetRotation = visionLayout.add("Vision Target Rotation", 0.0).entry

    val visionDriveAngle = visionLayout.add("Vision Drive Angle", 0.0).entry
    val visionDriveActive = visionLayout.add("Vision Drive Active", false).entry

    init {
        // Put choosers on dashboard
        autoLayout.add(
            "Auto Mode",
            autoModeChooser
        )
        autoLayout.add(
            "Starting Position",
            startingPositionChooser
        )

        //mainShuffleboardDisplay.add(VisionProcessing.cameraSource).withPosition(3, 2).withSize(3, 3)
    }

    fun update() {
        globalXEntry.setDouble(DriveSubsystem.localization().translation.x.feet)
        globalYEntry.setDouble(DriveSubsystem.localization().translation.y.feet)
        globalAEntry.setDouble(DriveSubsystem.localization().rotation.degree)

        leftPositionEntry.setDouble(DriveSubsystem.leftMotor.getSelectedSensorPosition(0).toDouble())
        rightPositionEntry.setDouble(DriveSubsystem.rightMotor.getSelectedSensorPosition(0).toDouble())

        leftAmperageEntry.setDouble(DriveSubsystem.leftMotor.outputCurrent)
        rightAmperageEntry.setDouble(DriveSubsystem.rightMotor.outputCurrent)

        val trackedObject = TargetTracker.bestTarget
        if (trackedObject != null) {
            val visionTargetPose = trackedObject.averagePose
            visionTargetX.setDouble(visionTargetPose.translation.x.inch)
            visionTargetY.setDouble(visionTargetPose.translation.y.inch)
            visionTargetRotation.setDouble(visionTargetPose.rotation.degree)
        }
    }
}