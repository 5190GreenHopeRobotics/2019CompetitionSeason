package org.ghrobotics.frc2019.auto.routines

import org.ghrobotics.frc2019.auto.paths.TrajectoryFactory
import org.ghrobotics.frc2019.auto.paths.TrajectoryWaypoints
import org.ghrobotics.frc2019.subsystems.Superstructure
import org.ghrobotics.frc2019.subsystems.drive.DriveSubsystem
import org.ghrobotics.frc2019.subsystems.intake.IntakeHatchCommand
import org.ghrobotics.frc2019.subsystems.intake.IntakeSubsystem
import org.ghrobotics.lib.commands.DelayCommand
import org.ghrobotics.lib.commands.parallel
import org.ghrobotics.lib.commands.sequential
import org.ghrobotics.lib.mathematics.twodim.trajectory.types.duration
import org.ghrobotics.lib.mathematics.units.Time
import org.ghrobotics.lib.mathematics.units.feet
import org.ghrobotics.lib.mathematics.units.second

class ForwardCargoShipRoutine : AutoRoutine() {

    private val path1 = TrajectoryFactory.centerStartToCargoShipFL
    private val path2 = TrajectoryFactory.cargoShipFLToLoadingStation
    private val path3 = TrajectoryFactory.loadingStationToCargoShipFR

    override val duration: Time
        get() = path1.duration + path2.duration + path3.duration

    override val routine
        get() = sequential {
            // Hold hatch
            // Put hatch on FL cargo ship
            +parallel {
                +IntakeHatchCommand(IntakeSubsystem.Direction.HOLD)
                +followVisionAssistedTrajectory(path1, { false }, 4.feet, true)
                +sequential {
                    +DelayCommand(path1.duration - 3.5.second)
                    +Superstructure.kFrontHatchFromLoadingStation.withTimeout(2.0.second)
                }
            }

            // Release hatch
            +IntakeHatchCommand(IntakeSubsystem.Direction.RELEASE)
            +DelayCommand(0.1.second)

            // Go to loading station
            +parallel {
                +followVisionAssistedTrajectory(path2, { false }, 5.feet)
                +sequential {
                    +DelayCommand(0.2.second)
                    +Superstructure.kBackHatchFromLoadingStation
                }
            }

            // Pickup hatch
            +relocalize(TrajectoryWaypoints.kLoadingStation, false, { false })
            +IntakeHatchCommand(IntakeSubsystem.Direction.HOLD)


            // Go to FR cargo ship
            +parallel {
                +followVisionAssistedTrajectory(path3, { false }, 3.feet, true)
                +sequential {
                    +executeFor(2.second, Superstructure.kStowedPosition)
                    +Superstructure.kFrontHatchFromLoadingStation.withTimeout(3.second)
                }
            }

            // Place hatch
            +IntakeHatchCommand(IntakeSubsystem.Direction.RELEASE)
            +DelayCommand(0.3.second)

            +DriveSubsystem.followTrajectory(TrajectoryFactory.cargoShipFLToLoadingStation)
        }
}