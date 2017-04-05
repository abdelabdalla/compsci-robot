package com.herokuapp.swype;

import java.io.IOException;
import java.util.ArrayList;

import com.ergotech.brickpi.BrickPi;
import com.ergotech.brickpi.motion.Motor;
import com.ergotech.brickpi.motion.MotorPort;
import com.ergotech.brickpi.sensors.SensorPort;
import com.ergotech.brickpi.sensors.TouchSensor;

public class Robot {

	//initialise motor's speed
    private int motorSpeed = 20;

    //get BrickPi data
    private final BrickPi brickPi = BrickPi.getBrickPi();

    private final Motor motorA = new Motor();
    private final Motor motorB = new Motor();
    private final Motor motorC = new Motor();
    private final Motor motorD = new Motor();
    
    //initialise initial angle
    private int angle = 0;

    public Robot(String[] commandStrings) throws IOException {
    	
    	System.out.println("Robot class received script");

    	//set up sensors
        brickPi.setSensor(new TouchSensor(), SensorPort.S1);
        brickPi.setSensor(new TouchSensor(), SensorPort.S2);
        brickPi.setSensor(new TouchSensor(), SensorPort.S3);
        brickPi.setSensor(new TouchSensor(), SensorPort.S4);

        //set up motors
        brickPi.setMotor(motorA, MotorPort.MA);
        brickPi.setMotor(motorB, MotorPort.MB);
        brickPi.setMotor(motorC, MotorPort.MC);
        brickPi.setMotor(motorD, MotorPort.MD);
        
        //reset drawing tool's location
        reset();

        //convert command into standard form
        Interpreter interpreter = new Interpreter();
        Interpreter.command[] commandTypes = interpreter.getCommandTypes(commandStrings);
        System.out.println("Script interpretted");

        ArrayList<Command> commands = new ArrayList<>();

        //convert commands into robot's form
        for (int i = 0; i < commandStrings.length; i++) {
        	System.out.println("Preparing script line " + (i+1) + " for printing");
            switch (commandTypes[i]) {
                case FWD:
                    commands.add(getCommandMagnitude(Interpreter.command.FWD,commandStrings[i]));
                    break;
                case RT:
                    commands.add(getCommandMagnitude(Interpreter.command.RT,commandStrings[i]));
                    break;
                case LT:
                    commands.add(getCommandMagnitude(Interpreter.command.LT,commandStrings[i]));
                    break;
                case PEN:
                    commands.add(getPenMagnitude(commandStrings[i]));
                    break;
                case REPEAT_STANDARD:
                    String repeatedStringS = commandStrings[i].split("\\[")[1].split("\\]")[0];
                    Command repeatCommandS = getCommandMagnitude(Interpreter.command.FWD,repeatedStringS);
                    int repS = getRep(commandStrings[i]);
                    ArrayList<Command> commandsRepeatedS = new ArrayList<>();
                    for (int j = 0; j < repS; j++) {
                        commandsRepeatedS.add(repeatCommandS);
                    }
                    commands.add(new Command(Interpreter.command.REPEAT_STANDARD, repS, commandsRepeatedS));
                    break;
                case REPEAT_PEN:
                    String repeatedStringP = commandStrings[i].split("\\[")[1].split("\\]")[0];
                    Command repeatCommandP = getPenMagnitude(repeatedStringP);
                    int repP = getRep(commandStrings[i]);
                    ArrayList<Command> commandsRepeatedP = new ArrayList<>();
                    for (int j = 0; j < repP; j++) {
                        commandsRepeatedP.add(repeatCommandP);
                    }
                    commands.add(new Command(Interpreter.command.REPEAT_PEN, repP, commandsRepeatedP));
                    break;
            }
        }
        //print commands
        print(commands);
    }

    //get magnitude of any command
    private Command getCommandMagnitude(Interpreter.command command, String commandString) {
        int magnitude = Integer.getInteger(commandString.replaceAll("\\D+", ""));
        return new Command(command, magnitude);
    }

    //get 'PEN[]' command direction
    private Command getPenMagnitude(String command) {
        String splitter = command.replaceAll("(?i)PEN", "").replaceAll("(?i)[^(DOWN|UP)]+", "");
        int magnitude = splitter.equalsIgnoreCase("up") ? 0 : 1;
        return new Command(Interpreter.command.PEN, magnitude);
    }

    //get repeated commands
    private int getRep(String command) {
        command = command.replace("(?i)REPEAT", "").replace("\\[.*", "").replaceAll(" ", "");
        return Integer.getInteger(command);
    }

    private void print(ArrayList<Command> commands) {
    	int i = 0;
    	//send single commands to begin moving motors
        for (Command command : commands) {
        	System.out.println("Printing line " + (i+1));
            switch (command.getCommand()) {
                case FWD:
                    fwd(command.getMagnitude());
                    break;
                case RT:
                case LT:
                    rotate(command.getCommand(),command.getMagnitude());
                case PEN:
                    pen(command.getMagnitude());
                    break;
                case REPEAT_STANDARD:
                case REPEAT_PEN:
                    print(command.getCommands());
                    break;
			case INVALID:
				break;
			default:
				break;
            }
        }
        reset();
    }

    //move motors for 'FWD' command
    private void fwd(int magnitude) {
        double x = magnitude * Math.sin(angle);
        double y = magnitude * Math.cos(angle);
        
        System.out.println("Moving forward y:" + y + ",x:" + x);

        motorA.rotate(y,motorSpeed);
        motorB.rotate(y,motorSpeed);
    }

    //set new angle for command direction
    private void rotate(Interpreter.command command, int magnitude) {
        angle += (command == Interpreter.command.RT ? 1 : -1) * magnitude;
        System.out.println("new angle: " + angle%90 + " degrees");
    }

    //move drawing tool up and down
    private void pen(int magnitude) {

        if (magnitude == 0) {
            while (brickPi.getSensor(SensorPort.S1).getValue() > 3) {
                motorD.rotate(2,motorSpeed);
            }
            System.out.println("lowering drawing tool");
        } else {
            motorD.rotate(4,-motorSpeed);
        	System.out.println("raising drawing tool");
        }
    }

    //move drawing tool to starting location
    private void reset() {
    	System.out.println("resetting location");
        pen(1);
        resetA.run();
        resetB.run();
        resetC.run();
    }

    private Runnable resetA = () -> {
        while (brickPi.getSensor(SensorPort.S1).getValue() == 0) {
            motorA.rotate(2,-motorSpeed);
        }
    };

    private Runnable resetB = () -> {
        while (brickPi.getSensor(SensorPort.S1).getValue() == 0) {
            motorB.rotate(2,-motorSpeed);
        }
    };

    private Runnable resetC = () -> {
        while (brickPi.getSensor(SensorPort.S1).getValue() == 0) {
            motorC.rotate(2,-motorSpeed);
        }
    };

}
