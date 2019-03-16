package org.ghrobotics.frc2019.subsystems.elevator

import com.ctre.phoenix.motorcontrol.*
import org.ghrobotics.frc2019.Constants
import org.ghrobotics.frc2019.Robot
import org.ghrobotics.frc2019.kMainLoopDt
import org.ghrobotics.frc2019.subsystems.EmergencyHandleable
import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.commands.FalconSubsystem
import org.ghrobotics.lib.mathematics.units.amp
import org.ghrobotics.lib.mathematics.units.derivedunits.acceleration
import org.ghrobotics.lib.mathematics.units.derivedunits.volt
import org.ghrobotics.lib.mathematics.units.inch
import org.ghrobotics.lib.mathematics.units.millisecond
import org.ghrobotics.lib.mathematics.units.nativeunits.nativeUnits
import org.ghrobotics.lib.utils.Source
import org.ghrobotics.lib.wrappers.ctre.AbstractFalconSRX
import org.ghrobotics.lib.wrappers.ctre.LinearFalconSRX
import org.ghrobotics.lib.wrappers.ctre.NativeFalconSRX
import kotlin.math.absoluteValue

/**
 * Represents the elevator of the robot.
 */
object ElevatorSubsystem : FalconSubsystem(), EmergencyHandleable {

    // 4 motors that move the elevator up and down.
    private val elevatorMaster = LinearFalconSRX(
        Constants.kElevatorMasterId,
        Constants.kElevatorNativeUnitModel
    )

    // List of all motors.
    private val allMotors: List<AbstractFalconSRX<*>>

    var wantedState: ElevatorState = ElevatorState.Nothing
    private var currentState: ElevatorState = ElevatorState.Nothing

    // Used to retrieve the current elevator position and to set the desired elevator position.
    var position = elevatorMaster.sensorPosition
        private set

    // Velocity of the elevator.
    var velocity = elevatorMaster.sensorVelocity
        private set

    // Acceleration of the elevator.
    var acceleration = 0.inch.acceleration
        private set

    // Current draw per motor.
    val current get() = elevatorMaster.outputCurrent

    // Raw encoder value.
    val rawEncoder get() = elevatorMaster.getSelectedSensorPosition(0)

    // Voltage draw per motor.
    val voltage get() = elevatorMaster.motorOutputPercent * 12.0

    // Checks if the limit switch is engaged
    val isBottomLimitSwitchPressed get() = elevatorMaster.sensorCollection.isRevLimitSwitchClosed

    var isZeroed = false
        private set

    var arbitraryFeedForward = 0.0
        private set

    init {
        val elevatorSlave1 = NativeFalconSRX(Constants.kElevatorSlave1Id)
        val elevatorSlave2 = NativeFalconSRX(Constants.kElevatorSlave2Id)
        val elevatorSlave3 = NativeFalconSRX(Constants.kElevatorSlave3Id)

        allMotors = listOf(elevatorMaster, elevatorSlave1, elevatorSlave2, elevatorSlave3)

        // Set slaves to follow master
        elevatorSlave1.follow(elevatorMaster)
        elevatorSlave2.follow(elevatorMaster)
        elevatorSlave3.follow(elevatorMaster)

        // Configure feedback sensor and sensor phase
        elevatorMaster.feedbackSensor = FeedbackDevice.QuadEncoder
        elevatorMaster.encoderPhase = false

        allMotors.forEach { motor ->
            // Brake mode
            motor.brakeMode = NeutralMode.Brake

            // Voltage compensation
            motor.voltageCompensationSaturation = 12.volt
            motor.voltageCompensationEnabled = true

            // Current limiting
            motor.peakCurrentLimit = 0.amp
            motor.peakCurrentLimitDuration = 0.millisecond
            motor.continuousCurrentLimit = Constants.kElevatorCurrentLimit
            motor.currentLimitingEnabled = true
        }

        // Configure startup settings
        elevatorMaster.also { motor ->
            // Limit switches
            motor.forwardLimitSwitchSource = LimitSwitchSource.FeedbackConnector to LimitSwitchNormal.NormallyOpen
            motor.reverseLimitSwitchSource = LimitSwitchSource.FeedbackConnector to LimitSwitchNormal.NormallyOpen
            motor.overrideLimitSwitchesEnable = true

            // Clear position when at bottom
            motor.clearPositionOnReverseLimitSwitch = true

            motor.softLimitForward = 10850.nativeUnits
            motor.softLimitForwardEnabled = true

            // Motion magic
            motor.motionCruiseVelocity = Constants.kElevatorCruiseVelocity
            motor.motionAcceleration = Constants.kElevatorAcceleration

            motor.setStatusFramePeriod(StatusFrame.Status_2_Feedback0, kMainLoopDt.millisecond.toInt())
            motor.configVelocityMeasurementPeriod(VelocityMeasPeriod.Period_20Ms)

//            motor.configMotionSCurveStrength(3)
        }

        // Default command to hold the current position
        defaultCommand = object : FalconCommand(this@ElevatorSubsystem) {
            override suspend fun initialize() {
                val currentState = this@ElevatorSubsystem.currentState
                val currentPosition = position.value
                val wantedPosition = if (currentState is ElevatorState.Position
                    && (currentState.position - currentPosition).absoluteValue <= Constants.kElevatorClosedLoopTolerance.value
                ) {
                    currentState.position
                } else if (currentState is ElevatorState.MotionMagic
                    && (currentState.position - currentPosition).absoluteValue <= Constants.kElevatorClosedLoopTolerance.value
                ) {
                    currentState.position
                } else {
                    currentPosition
                }
                wantedState = ElevatorState.Position(wantedPosition)
            }
        }

        // Set closed loop gains
        setClosedLoopGains()
    }

    /**
     * Configures closed loop gains for the elevator.
     */
    private fun setClosedLoopGains() {
        allMotors.forEach { motor ->
            motor.config_kP(0, Constants.kElevatorKp)
            motor.config_kD(0, Constants.kElevatorKd)
            motor.config_kF(0, Constants.kElevatorKf)

            motor.config_kP(1, Constants.kElevatorKp)
            motor.config_kD(1, Constants.kElevatorKd)
            motor.config_kF(1, 0.0)
        }
    }

    /**
     * Zeros all feedback gains for the elevator.
     */
    private fun zeroClosedLoopGains() {
        allMotors.forEach { motor ->
            motor.config_kP(0, 0.0)
            motor.config_kD(0, 0.0)

            motor.config_kP(1, 0.0)
            motor.config_kD(1, 0.0)
        }
    }

    override fun zeroOutputs() {
        wantedState = ElevatorState.Nothing
    }

    /**
     * Runs periodically.
     * Used to calculate the acceleration of the elevator.
     */
    override fun periodic() {
        val previousVelocity = velocity

        position = elevatorMaster.sensorPosition
        velocity = elevatorMaster.sensorVelocity
        acceleration = (velocity - previousVelocity) / kMainLoopDt

        if (isBottomLimitSwitchPressed) {
            isZeroed = true
        }

        arbitraryFeedForward =
            if (position >= Constants.kElevatorSwitchHeight || Robot.emergencyActive) {
                Constants.kElevatorAfterSwitchKg
            } else {
                Constants.kElevatorBelowSwitchKg
            }

        val wantedState = this.wantedState
        val previousState = this.currentState
        this.currentState = wantedState
        when (wantedState) {
            is ElevatorState.Nothing -> {
                elevatorMaster.set(ControlMode.Disabled, 0.0)
            }
            is ElevatorState.MotionMagic -> {
                if (previousState !is ElevatorState.MotionMagic) {
                    elevatorMaster.selectProfileSlot(0, 0)
                }
                elevatorMaster.set(
                    ControlMode.MotionMagic,
                    Constants.kElevatorNativeUnitModel.toNativeUnitPosition(wantedState.position),
                    DemandType.ArbitraryFeedForward,
                    arbitraryFeedForward
                )
            }
            is ElevatorState.Position -> {
                if (previousState !is ElevatorState.Position) {
                    elevatorMaster.selectProfileSlot(1, 0)
                }
                elevatorMaster.set(
                    ControlMode.Position,
                    Constants.kElevatorNativeUnitModel.toNativeUnitPosition(wantedState.position),
                    DemandType.ArbitraryFeedForward,
                    arbitraryFeedForward
                )
            }
            is ElevatorState.OpenLoop -> {
                if (wantedState.useFeedForward) {
                    elevatorMaster.set(
                        ControlMode.PercentOutput,
                        wantedState.output(),
                        DemandType.ArbitraryFeedForward,
                        arbitraryFeedForward
                    )
                } else {
                    elevatorMaster.set(ControlMode.PercentOutput, wantedState.output())
                }
            }
        }
    }

    // Emergency Management
    override fun activateEmergency() {
        zeroOutputs()
        zeroClosedLoopGains()
    }

    override fun recoverFromEmergency() = setClosedLoopGains()

    sealed class ElevatorState {
        object Nothing : ElevatorState()
        class MotionMagic(val position: Double) : ElevatorState()
        class Position(val position: Double) : ElevatorState()
        class OpenLoop(val output: Source<Double>, val useFeedForward: Boolean) : ElevatorState() {
            constructor(output: Double, useFeedForward: Boolean) : this(Source(output), useFeedForward)
        }
    }
}