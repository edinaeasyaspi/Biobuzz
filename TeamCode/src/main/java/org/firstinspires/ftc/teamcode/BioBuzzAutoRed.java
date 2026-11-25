package org.firstinspires.ftc.teamcode;

import static com.qualcomm.robotcore.hardware.DcMotor.ZeroPowerBehavior.BRAKE;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.util.ElapsedTime;

/*
 * BioBuzz StarterBot Autonomous - RED
 *
 * Follows the path from the drawing:
 *   1. Start in the red corner square, robot facing up the field (launcher at the front).
 *   2. Drive forward, curving slightly to the right.
 *   3. Turn right ~90 degrees so the launcher points toward the goal.
 *   4. Spin up the launcher and feed balls with the windmill + intake.
 *
 * This is TIME based (no drive encoders needed on the StarterBot). All the numbers in the
 * "TUNING" section will need adjusting on your real robot / battery / floor.
 */
@Autonomous(name = "BioBuzz Auto RED", group = "StarterBot")
public class BioBuzzAutoRed extends OpMode {

    // ---------- Hardware (same names as the teleop) ----------
    private DcMotor leftDrive = null;
    private DcMotor rightDrive = null;
    private DcMotorEx launcher = null;
    private DcMotor intake = null;
    private CRServo leftIntakeServo = null;
    private CRServo rightIntakeServo = null;
    private CRServo windmillServo = null;

    // ---------- Launcher values copied from the teleop ----------
    public final int LAUNCHER_TARGET_VELOCITY = 1300;
    public final int LAUNCHER_MIN_VELOCITY = 1250;

    // ---------- TUNING ----------
    // +1 = turn right (clockwise) for RED as drawn. The BLUE file uses -1 (mirror image).
    final double TURN_DIRECTION = 1.0;

    final double DRIVE_POWER = 0.5;
    final double TURN_POWER = 0.4;

    // Curve at the start of the path: left wheel faster than right = veer right.
    final double CURVE_LEFT_POWER = 0.55;
    final double CURVE_RIGHT_POWER = 0.40;

    final double DRIVE_FORWARD_TIME = 1.2;   // seconds, straight-ish drive from the start square
    final double CURVE_TIME = 0.8;           // seconds, the curve in the drawing
    final double TURN_TIME = 0.6;            // seconds to turn about 90 degrees
    final double CREEP_FORWARD_TIME = 0.3;   // seconds, small final approach to the shooting spot
    final double SPIN_UP_TIMEOUT = 3.0;      // max seconds to wait for launcher speed
    final double LAUNCH_TIME = 8.0;          // seconds of feeding balls into the launcher

    // ---------- State machine ----------
    private enum State {
        DRIVE_FORWARD,
        CURVE,
        TURN_TO_GOAL,
        CREEP,
        SPIN_UP,
        LAUNCH,
        DONE
    }

    private State state = State.DRIVE_FORWARD;
    private final ElapsedTime stateTimer = new ElapsedTime();

    @Override
    public void init() {
        leftDrive = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");
        intake = hardwareMap.get(DcMotor.class, "intake");
        launcher = hardwareMap.get(DcMotorEx.class, "launcher");
        windmillServo = hardwareMap.get(CRServo.class, "windmill");
        leftIntakeServo = hardwareMap.get(CRServo.class, "left_intake_servo");
        rightIntakeServo = hardwareMap.get(CRServo.class, "right_intake_servo");

        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);

        leftDrive.setZeroPowerBehavior(BRAKE);
        rightDrive.setZeroPowerBehavior(BRAKE);
        intake.setZeroPowerBehavior(BRAKE);

        launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        launcher.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER,
                new PIDFCoefficients(40, 0, 0, 12.5));

        leftIntakeServo.setPower(0);
        rightIntakeServo.setPower(0);
        windmillServo.setPower(0);

        rightIntakeServo.setDirection(DcMotorSimple.Direction.REVERSE);
        windmillServo.setDirection(DcMotorSimple.Direction.REVERSE);

        telemetry.addData("Status", "Initialized - RED auto");
    }

    @Override
    public void start() {
        stateTimer.reset();
        state = State.DRIVE_FORWARD;
    }

    @Override
    public void loop() {
        switch (state) {
            case DRIVE_FORWARD:
                setDrive(DRIVE_POWER, DRIVE_POWER);
                if (stateTimer.seconds() >= DRIVE_FORWARD_TIME) {
                    nextState(State.CURVE);
                }
                break;

            case CURVE:
                // Mirror the curve for the other alliance
                if (TURN_DIRECTION > 0) {
                    setDrive(CURVE_LEFT_POWER, CURVE_RIGHT_POWER);
                } else {
                    setDrive(CURVE_RIGHT_POWER, CURVE_LEFT_POWER);
                }
                if (stateTimer.seconds() >= CURVE_TIME) {
                    nextState(State.TURN_TO_GOAL);
                }
                break;

            case TURN_TO_GOAL:
                // Spin in place: right turn = left wheel forward, right wheel backward
                setDrive(TURN_POWER * TURN_DIRECTION, -TURN_POWER * TURN_DIRECTION);
                if (stateTimer.seconds() >= TURN_TIME) {
                    nextState(State.CREEP);
                }
                break;

            case CREEP:
                setDrive(DRIVE_POWER * 0.6, DRIVE_POWER * 0.6);
                if (stateTimer.seconds() >= CREEP_FORWARD_TIME) {
                    setDrive(0, 0);
                    nextState(State.SPIN_UP);
                }
                break;

            case SPIN_UP:
                setDrive(0, 0);
                launcher.setVelocity(LAUNCHER_TARGET_VELOCITY);
                if (launcher.getVelocity() > LAUNCHER_MIN_VELOCITY
                        || stateTimer.seconds() >= SPIN_UP_TIMEOUT) {
                    nextState(State.LAUNCH);
                }
                break;

            case LAUNCH:
                // Same behavior as the teleop: only feed when the launcher is fast enough
                launcher.setVelocity(LAUNCHER_TARGET_VELOCITY);
                if (launcher.getVelocity() > LAUNCHER_MIN_VELOCITY) {
                    windmillServo.setPower(1);
                    setIntake(0.5);
                } else {
                    windmillServo.setPower(0);
                    setIntake(0);
                }
                if (stateTimer.seconds() >= LAUNCH_TIME) {
                    nextState(State.DONE);
                }
                break;

            case DONE:
            default:
                setDrive(0, 0);
                launcher.setVelocity(0);
                windmillServo.setPower(0);
                setIntake(0);
                break;
        }

        telemetry.addData("State", state);
        telemetry.addData("State time", "%.2f", stateTimer.seconds());
        telemetry.addData("Launcher velocity", "%.0f", launcher.getVelocity());
        telemetry.update();
    }

    @Override
    public void stop() {
        setDrive(0, 0);
        launcher.setVelocity(0);
        windmillServo.setPower(0);
        setIntake(0);
    }

    private void nextState(State newState) {
        state = newState;
        stateTimer.reset();
    }

    private void setDrive(double left, double right) {
        leftDrive.setPower(left);
        rightDrive.setPower(right);
    }

    private void setIntake(double power) {
        intake.setPower(power);
        leftIntakeServo.setPower(power);
        rightIntakeServo.setPower(power);
    }
}
