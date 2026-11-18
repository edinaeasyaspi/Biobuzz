package org.firstinspires.ftc.teamcode;
// the import thingy mabobbers are so that I don't have to write a billion more lines of code so probably best
//not to change them
import static com.qualcomm.robotcore.hardware.DcMotor.ZeroPowerBehavior.BRAKE;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.util.ElapsedTime;

@Autonomous(name = "BioBuzz StarterBot Auto (Right Side)", group = "StarterBot")
public class BioBuzzStarterbotAuto_RightSide extends LinearOpMode {

    // Declare OpMode members (matching BioBuzzStarterbotTeleop)
    private DcMotor leftDrive = null;
    private DcMotor rightDrive = null;
    private DcMotorEx launcher = null;
    private DcMotor intake = null;
    private CRServo leftIntakeServo = null;
    private CRServo rightIntakeServo = null;
    private CRServo windmillServo = null;

    private ElapsedTime runtime = new ElapsedTime();

    // Exact launcher velocity settings from the TeleOp
    // Do not change these variables as the CRservo will not go any higher.
    public final int LAUNCHER_TARGET_VELOCITY = 1250;
    public final int LAUNCHER_MIN_VELOCITY = 1200;

    // Driving speeds for Autonomous
    // These can be adjusted
    static final double FORWARD_SPEED = 0.6;
    static final double TURN_SPEED = 0.5;

    @Override
    public void runOpMode() {
        // 1. Hardware Mapping (Exact strings from the TeleOp)
        //For the configure the control will do just fine and nothing will need to be updated.
        leftDrive        = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive       = hardwareMap.get(DcMotor.class, "right_drive");
        intake           = hardwareMap.get(DcMotor.class, "intake");
        launcher         = hardwareMap.get(DcMotorEx.class, "launcher");
        windmillServo    = hardwareMap.get(CRServo.class, "windmill");
        leftIntakeServo  = hardwareMap.get(CRServo.class, "left_intake_servo");
        rightIntakeServo = hardwareMap.get(CRServo.class, "right_intake_servo");

        // 2. Motor & Servo Directions (Exact directions from the TeleOp)
        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);

        rightIntakeServo.setDirection(DcMotorSimple.Direction.REVERSE);
        windmillServo.setDirection(DcMotorSimple.Direction.REVERSE);

        // 3. Brake Modes (Exact behavior from the TeleOp)
        leftDrive.setZeroPowerBehavior(BRAKE);
        rightDrive.setZeroPowerBehavior(BRAKE);
        intake.setZeroPowerBehavior(BRAKE);

        // 4. Launcher Velocity & Custom PIDF Tuning (Exact values from the TeleOp)
        launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        launcher.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, new PIDFCoefficients(40, 0, 0, 12.5));

        // 5. Initialize Servos to 0
        leftIntakeServo.setPower(0);
        rightIntakeServo.setPower(0);
        windmillServo.setPower(0);

        telemetry.addData("Status", "BioBuzz Right-Side Auto Initialized");
        telemetry.update();

        // Wait for driver to press START
        waitForStart();

        // STEP 1: Spin up launcher immediately so it reaches 1250 ticks/sec while driving
        launcher.setVelocity(LAUNCHER_TARGET_VELOCITY);

        // Drive forward toward shooting spot (2.0 seconds - tune on field)
        driveByTime(FORWARD_SPEED, FORWARD_SPEED, 2.0, "Driving to Hive");

        // STEP 2: Turn LEFT toward the Hive (1.0 second - tune on field)
        driveByTime(-TURN_SPEED, TURN_SPEED, 1.0, "Aiming Left at Hive");
        stopDrive();

        // STEP 3: Shoot pre-loaded Pollen for 4.0 seconds
        runtime.reset();
        while (opModeIsActive() && (runtime.seconds() < 4.0)) {
            double currentVel = launcher.getVelocity();

            if (currentVel >= LAUNCHER_MIN_VELOCITY) {
                windmillServo.setPower(1.0);
                intake.setPower(0.8);
                leftIntakeServo.setPower(0.8);
                rightIntakeServo.setPower(0.8);
            } else {
                windmillServo.setPower(0);
            }

            telemetry.addData("Status", "Shooting Pollen (%4.1f S)", runtime.seconds());
            telemetry.addData("Launcher Vel", "%.0f / %d", currentVel, LAUNCHER_TARGET_VELOCITY);
            telemetry.update();
        }

        // STEP 4: Turn off shooter, windmill, and intake
        windmillServo.setPower(0);
        intake.setPower(0);
        leftIntakeServo.setPower(0);
        rightIntakeServo.setPower(0);
        launcher.setVelocity(0);

        // STEP 5: Back up / Park (1.0 second - tune on field)
        driveByTime(-FORWARD_SPEED, -FORWARD_SPEED, 1.0, "Parking");
        stopDrive();

        telemetry.addData("Status", "Autonomous Complete");
        telemetry.update();
    }

    // Helper method for time-based driving
    private void driveByTime(double leftSpeed, double rightSpeed, double seconds, String stepName) {
        leftDrive.setPower(leftSpeed);
        rightDrive.setPower(rightSpeed);
        runtime.reset();
        while (opModeIsActive() && (runtime.seconds() < seconds)) {
            telemetry.addData("Step", "%s (%4.1f S)", stepName, runtime.seconds());
            telemetry.addData("Launcher Vel", "%.0f", launcher.getVelocity());
            telemetry.update();
        }
    }

    private void stopDrive() {
        leftDrive.setPower(0);
        rightDrive.setPower(0);
    }
}
