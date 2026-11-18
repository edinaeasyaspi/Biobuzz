package org.firstinspires.ftc.teamcode;
// Again thingys that make life easier no no change
import static com.qualcomm.robotcore.hardware.DcMotor.ZeroPowerBehavior.BRAKE;
//Any teams that use this code will probably have to change the variables because the leftDrive and rightDrive Servos
//Will Probably have to be configured so that it is compatible with your driver hub.
//Author: PaxClang
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.util.ElapsedTime;
//the import takes previous code from ftc and brings it into this so it makes life significantly easier
@Autonomous(name = "BioBuzz StarterBot Auto (Left Side)", group = "StarterBot")

public class BioBuzzStarterbotAuto_LeftSide extends LinearOpMode {

    // Declare OpMode memb'
    // ers (matching BioBuzzStarterbotTeleop)
    //anyone using the code will have to chanage this so that it matches your teleop code
    //the drive motors are not compatible with other driver hubs so if u use it and not change
    //It will break the code and not work.
    private DcMotor leftDrive = null;
    private DcMotor rightDrive = null;
    private DcMotorEx launcher = null;
    private DcMotor intake = null;
    private CRServo leftIntakeServo = null;
    private CRServo rightIntakeServo = null;
    private CRServo windmillServo = null;

    private ElapsedTime runtime = new ElapsedTime();

    // Exact launcher velocity settings from the TeleOp
    //If u don't have CRServos you will have to tweak it because the velocity may be different.
    public final int LAUNCHER_TARGET_VELOCITY = 1250;
    public final int LAUNCHER_MIN_VELOCITY = 1200;

    // Driving speeds for Autonomous
    //Probably best not to change this
    static final double FORWARD_SPEED = 0.6;
    static final double TURN_SPEED = 0.5;

    @Override
    public void runOpMode() {
        // 1. Hardware Mapping (Exact strings from the TeleOp)
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

        // 4. Launcher Velocity & Custom PIDF Tuning (Exact values from thee TeleOp)
        launcher.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        launcher.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, new PIDFCoefficients(40, 0, 0, 12.5));

        // 5. Initialize Servos to 0
        leftIntakeServo.setPower(0);
        rightIntakeServo.setPower(0);
        windmillServo.setPower(0);

        telemetry.addData("Status", "BioBuzz Auto Initialized");
        telemetry.update();

        // Wait for driver to press START
        waitForStart();

        // STEP 1: Spin up launcher immediately so it reaches 1250 ticks/sec while driving
        launcher.setVelocity(LAUNCHER_TARGET_VELOCITY);

        // Drive forward toward shooting spot (2.0 seconds - tune on field)
        driveByTime(FORWARD_SPEED, FORWARD_SPEED, 2.0, "Driving to Hive");

        // STEP 2: Turn toward the Hive (1.0 second - tune on field)
        driveByTime(TURN_SPEED, -TURN_SPEED, 1.0, "Aiming at Hive");
        stopDrive();

        // STEP 3: Shoot pre-loaded Pollen for 4.0 seconds
        // Just like the TeleOp, only runs the windmill when launcher velocity >= 1200!
        //Do not tinker with the velocity it is set what it is for a reason.
        runtime.reset();
        while (opModeIsActive() && (runtime.seconds() < 4.0)) {
            double currentVel = launcher.getVelocity();

            if (currentVel >= LAUNCHER_MIN_VELOCITY) {
                windmillServo.setPower(1.0);
                intake.setPower(0.8);
                leftIntakeServo.setPower(0.8);
                rightIntakeServo.setPower(0.8);
            } else {
                // Pause feeding briefly if flywheel slows down below 1200 ticks/sec
                windmillServo.setPower(0);
            }

            telemetry.addData("Status", "Shooting Pollen (%4.1f S)", runtime.seconds());
            telemetry.addData("Launcher Vel", "%.0f / %d", currentVel, LAUNCHER_TARGET_VELOCITY);
            telemetry.update();
        }

        // STEP 4: Turn off shooter, windmill, and intake
        //cut power to all parts before stopping.
        windmillServo.setPower(0);
        intake.setPower(0);
        leftIntakeServo.setPower(0);
        rightIntakeServo.setPower(0);
        launcher.setVelocity(0);

        // STEP 5: Back up / Park (1.0 second - tune on field)
        driveByTime(-FORWARD_SPEED, -FORWARD_SPEED, 1.0, "Parking");
        stopDrive();
        //ending for the autonomous no need to press the stop button this will broadcast that information to the control
        //hub
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
//This Code was created by team 8034 and is allowed for use for other teams a few things will have to be changed
//If you decide to use this because many variables are compatible only with team 8034 code.
//after the code is put onto the Control Hub remember to configure in the driver hub or it will not work