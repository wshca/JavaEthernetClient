/* Wolfgang Huettinger
 * StudentID: 
 * Email: 
 * CIS 2460 
 * Assignment 2 
 */
import java.util.*;
import java.math.*;

public class Ethernet 
{
	private int frameLength; //length of the frame
	private int intArrTime; //the interarrivalTime between frames
	private int numDevices;
	private int status;  //indicates the status of a frame
		
	//randomly generates an random integer
	private int genInteger (int seed)
	{
		int randomNumber;
		if (seed != 0) { //reseed if the given seed is not 0
			Random rand = new Random(seed);
			randomNumber = rand.nextInt();
		}
		else {
			Random rand = new Random();
			randomNumber = rand.nextInt();
		}
		//call random function and return it
		return randomNumber;
	}
	
	//randomly generates an random double
	private double genDouble (int seed)
	{
		double randomNumber;
		if (seed != 0) { //reseed if the given seed is not 0
			Random rand = new Random(seed);
			randomNumber = rand.nextDouble();
		}
		else {
			Random rand = new Random();
			randomNumber = rand.nextDouble();
		}
		//call random function and return it
		return randomNumber;
	}
	
	// back-off algorithm exponential
	private double backoffExponential (int seed, int numberOfCollisions)
	{
		double backoffTime; // for more oversight I use additional variables 
		double modulo;
		modulo = Math.pow(2,numberOfCollisions) - 1.0;    //calculate 2^m-1
		backoffTime = genDouble(seed) % modulo;           //get a number beween 0 and 2^m-1
		return backoffTime;                               //return the back-off time
	}
	
	// back-off algorithm fixed
	// difference: it is going to be lower in CPU usage because it does not need to calculate a 
	// random number and multiply the back-off time. The likelyhood of having the same back-off 
	// time for several devices is higher and therefore the number of collisions is going to be
	// higher and therefore the throughput lower
	private double backoffFixed (int waitTimeFixed, int deviceNumber)
	{
		int backoffTime;						    //local variable to be nice 
		backoffTime = waitTimeFixed * deviceNumber; //calculate the back-off time 
		return (double)backoffTime;                 //make it double and return it
	}
	
	// print first input statements to the screen
	private int printInput(int seed, int numberOfDevices, int numberOfFrames, double arrivalRate)
	{
		double meanInterarrivalTime = 10 / arrivalRate * 256;
		System.out.println("Seed = " + seed);
		System.out.println("Number of devides = " + numberOfDevices);
		System.out.println("Number of frames to simulate = " + numberOfFrames);
		System.out.println("Mean # arrival frames/100 slot times = " + arrivalRate);
		System.out.println("Mean interarrival time = " + meanInterarrivalTime + " microsec (mus)");
		System.out.println(""); //empty line
		return 0;
	}
	
	// print system run messages
	private int printSystemRun(int frameNumber, int systemTimeInt, int eventType, int frameLen, int frameCol, int frameSdr, String frameSta, double frameTrantm, double backOffTime)
	{
		double systemTime = (double)systemTimeInt;
		systemTime = systemTime /10;
		if (eventType == 1){ //arrival of frame 
			System.out.println("Frame " + frameNumber + " arrives at " + systemTime + " mus"); //line 1
			System.out.println("    Frame " + frameNumber + ": len=" + frameLen + " col=" + frameCol + " sdr=" + frameSdr + " sta=" + frameSta + " atm=" + systemTime + " trantm=" + frameTrantm);
		}
		
		if (eventType == 2){ //sender of frame starts listening
			System.out.println("Frame " + frameNumber + " sender listens at " + systemTime + " mus... " + frameSta); //line 1
		}
		
		if (eventType == 3){ //sender of frame ends transmission of first 72 bytes
			System.out.println("Frame " + frameNumber + " 72 bytes send at " + systemTime + " mus... ");
		}
		
		if (eventType == 4){ //transmissions of frame is completed
			System.out.println("Frame " + frameNumber + " sending completed at " + systemTime + " mus... "); 
		}
		
		if (eventType == 5){ //frame is dropped
			System.out.println("Frame " + frameNumber + " is dropped at " + systemTime + " mus... "); 
		}
		
		if (eventType == 11){ //notify collision detected by sender and tells the backoff time
			System.out.println("Frame " + frameNumber + " sender detects " + frameCol + "'th collision at " + systemTime + " mus... "); 
			System.out.println("Frame " + frameNumber + " sender backs off " + backOffTime + " mus... "); 
		}
		
		if (eventType == 12){ //notify that the sender listens
			System.out.println("Frame " + frameNumber + " sender listens at " + systemTime + " mus... " + frameSta); 
		}
				
		return 0;
	}
	
	// print performance report
	private int printPerformance(double sentFrames, int droppedFrames, double networkThroughput)
	{
		double roundNetwork = networkThroughput / 1000000; //devide by 10^6 to get from bit to Mbit
		roundNetwork = Math.round(roundNetwork * 1) / 1;   //round the number to one digit after
		System.out.println(""); //empty line
		System.out.println("Performance Report:");
		System.out.println("Frames sent successfully = " + sentFrames + " bytes");
		System.out.println("Number of frames dropped = " + droppedFrames); 
		System.out.println("Network       throughput = " + networkThroughput + " bps = " + roundNetwork + " Mbps");
		return 0;
	}
	
	// function to get the length of a frame in the speciefied range
	private int interarrivalTime(int seed, double arrivalRate)
	{
		double interTime;
		double uniformRand;
		//exponentially distributed number is explained here:
		//http://en.wikipedia.org/wiki/Exponential_distribution#Generating_exponential_variates
		uniformRand = genDouble(seed) % 1;                  //we only want to have one between 0 and 1
		interTime = -1 * Math.log(uniformRand) / (arrivalRate * 10.0); 
		return (int)interTime;                              //cast and return it
	}
	
	// function to get the length of a frame in the speciefied range
	private int frameLength(int seed)
	{
		int length;
		length = genInteger(seed) % (1526-72);
		while (length < 0 || length > (1526-72)) {
			length = genInteger(seed) % (1526-72);
		}
		length = length + 72;
		return length;
	}
	
	// the actual program which does the simulation
	private int runSimulation(int seed, int numberOfDevices, int numberOfFrames, double arrivalRate)
	{
		//local variables for the frames - see it as a matrix (vectors are used to make it better to read)
		int frameLen[] = new int[numberOfFrames];           //length of the frame
		int frameCol[] = new int[numberOfFrames];           //number of collisions
		int frameSdr[] = new int[numberOfFrames];		    //frame sender ID
		String frameSta[] = new String[numberOfFrames];     //status of the cable at the time of a call
		//double frameAtm[] = new double[numberOfFrames];     //time of the system - when the frame was successfully transmitted
		double frameTrantm[] = new double[numberOfFrames];  //I have no idea what trantm shows - could not reverse engineer that part in the assignment
		int frameArrival[] = new int[numberOfFrames]; //time the frame is generated
		//local variables for the devices structure
		int deviceInUse[] = new int[numberOfDevices];		//denotes if the device is currently is use

		//other local variables
		int i;
		int j;
		int k;
		int m;
		int n;
		int saveArrivalTime = 0;
		int saveFrameLen = 0;
		int saveFrameLength[] = new int[numberOfFrames];
		int countFrameParts = 0;
		int backOffTime = 0;
		double backOffTimeDouble;
		int tempInt;
		int systemTime; 		//local time is in mus*10 - but to make it easier we use int and then when we call a subroutine with a double the integer is devided by 10
		int systemTimeMax; 		//maximum the system time can be to avoid for-ever-loops
		int storeSystemTime; 	//store the current system time
		int framesSent = 0;
		int framesDropt = 0;
		double networkthroughput = 0;
		int listenonce = 0;
		
		//generate the frames in the simulation
		for (i=0;i<numberOfFrames;i++){
			frameLen[i] = frameLength(seed);							//length of the frame in bytes
			frameCol[i] = 0;											//number of collisions is 0 at start
			tempInt = genInteger(seed) % numberOfDevices;
			while (tempInt < 0 || tempInt > numberOfDevices) {
				tempInt = genInteger(seed) % numberOfDevices;
			}
			frameSdr[i] = tempInt;		//frame sender ID
			if (i==0) { //adding times only after the first frame
				frameArrival[i] = interarrivalTime(seed, arrivalRate);	//caculate the interarrival time for the first frame 
			}
			else {
				frameArrival[i] = frameArrival[i-1] + interarrivalTime(seed, arrivalRate);	//caculate the interarrival time for all following frames
			}
		}
		
		for (i=0;i<numberOfDevices;i++) {
			deviceInUse[i] = 0;
		}
		
		//write the input data out
		printInput(seed, numberOfDevices, numberOfFrames, arrivalRate);
		
		//set the values
		i = 0;					//denotes the current frame in progress
		j = 0;
		k = 0;
		m = 0;
		countFrameParts = 0;
		systemTime = 0;			//denotes the time in mus*10 so we can use integer
		systemTimeMax = 10000000; //denotes the maximum number of the system time to avoid problems
		storeSystemTime = 0;	//to make java compiler happy
		//call the loop to count time and finish when all frames are processed or systemTime reached its maximum to avoid for-ever-loops
		while (i<numberOfFrames && systemTime < systemTimeMax) {
			if (systemTime > frameArrival[i]) {

				if (countFrameParts > 0) {
					deviceInUse[frameSdr[i]] = 0;
				}
				
				if (m == 0) {										//wait the 1 second
					printSystemRun(i+1, systemTime, 1, frameLen[i], frameCol[i], frameSdr[i]+1, "listen", frameTrantm[i], 0);
					frameArrival[i] = frameArrival[i] + 10;
					m++;
					systemTime = systemTime + 10;
				}
				if (deviceInUse[frameSdr[i]] == 0) { 				//listening to the cable
					if (listenonce == 0){
						printSystemRun(i+1, systemTime, 12, frameLen[i], frameCol[i], frameSdr[i]+1, "quiet", frameTrantm[i], 0);
						listenonce++;
					}
				}
				else {
					if (listenonce == 0){
						printSystemRun(i+1, systemTime, 12, frameLen[i], frameCol[i], frameSdr[i]+1, "busy", frameTrantm[i], 0);
						listenonce++;
					}
				}
				
				//if (deviceInUse[frameSdr[i]] == 0) {				//make sure sender is free
					if (countFrameParts == 0) {
						m = 0;
						listenonce = 0;
						saveArrivalTime = frameArrival[i];
						saveFrameLen = frameLen[i];
						saveFrameLength[i] = frameLen[i];
						printSystemRun(i+1, systemTime, 3, frameLen[i], frameCol[i], frameSdr[i]+1, "", frameTrantm[i], 0);
					}
					countFrameParts++;
					
					deviceInUse[frameSdr[i]] = 0; 					//set the sender to 1
					frameTrantm[i] = 0;			 					//start counter of time
					frameLen[i] = frameLen[i] - 72; 				//subtract the transmitted part
					frameArrival[i] = frameArrival[i] + 256;		//add 25.6 for mus the signal takes for one section
					storeSystemTime = systemTime;					//store the time we started transmitting
					j = 1;											//set j to 1 so we start the blocking sequence to the right
					k = 1;											//set j to 1 so we start the blocking sequence to the left
				}
			
			
			if (systemTime == storeSystemTime + j && j != 0) {		//for the right direction
				if (frameSdr[i] + j == numberOfDevices) {
					j = 0;										//stop this part if we reached the end of the devices on the right side
				}
				else {
					if (deviceInUse[frameSdr[i] + j] != 1) {			//make sure the device is free to be blocked
					deviceInUse[frameSdr[i] + j] = 1;	
					j++;
					}
					else {												//collision is detected
						countFrameParts = 0;
						listenonce = 0;
						frameLen[i] = saveFrameLen;
						frameCol[i]++;
						backOffTimeDouble = backoffExponential(seed,frameCol[i]);
						//backOffTimeDouble = backoffFixed(100,frameSdr[i] + j) * 10; 	//alternative backOffAlgorithm
						backOffTime = (int)backOffTimeDouble;
						frameArrival[i] = systemTime + backOffTime;		//back off time added to the original starttime
						printSystemRun(i+1, systemTime, 11, frameLen[i], frameCol[i], frameSdr[i]+1, "", frameTrantm[i], (double)backOffTime/10);
						for (n=0;n<numberOfDevices;n++) {
							deviceInUse[n] = 0;
						}
					}
				}
			}//end of if statement system == storeSystemTime + j
			
			if (systemTime == storeSystemTime - k && k != 0) {		//for the left direction
				if (frameSdr[i] - k == 1) {
					k = 0;										//stop this part if we reached the end of the devices on the left side
				}
				else {
					if (deviceInUse[frameSdr[i] - k] != 1) {			//make sure the device is free to be blocked
					deviceInUse[frameSdr[i] - k] = 1;				//for the left direction
					k++;
					}
					else {												//collision is detected
						countFrameParts = 0;
						listenonce = 0;
						frameLen[i] = saveFrameLen;
						frameCol[i]++;
						backOffTimeDouble = backoffExponential(seed,frameCol[i]);
						//backOffTimeDouble = backoffFixed(100,frameSdr[i] + k) * 10; 	//alternative backOffAlgorithm
						backOffTime = (int)backOffTimeDouble;
						frameArrival[i] = systemTime + backOffTime;		//back off time added to the original starttime
						printSystemRun(i+1, systemTime, 11, frameLen[i], frameCol[i], frameSdr[i]+1, "", frameTrantm[i], (double)backOffTime/10);
						for (n=0;n<numberOfDevices;n++) {
							deviceInUse[n] = 0;
						}
					}
				}
			}

				
			
			
			
			//drop frame if number of collisions reaches 10
			if (frameCol[i] == 10) {
				printSystemRun(i+1, systemTime, 5, frameLen[i], frameCol[i], frameSdr[i], "", frameTrantm[i], 0);
				i++;												//frame is dropped, let's go to the next one
				countFrameParts = 0;								//reset some values to go ahead
				for (m=0;m<numberOfDevices;m++) {
					deviceInUse[m] = 0;						
				}
				j = 0;
				k = 0;
								
			}
			else {
			
			//check if the frame is finished transmitting
			if (frameLen[i] < 1) {
				printSystemRun(i+1, systemTime, 4, frameLen[i], frameCol[i], frameSdr[i], "", frameTrantm[i], backOffTime);
				i++;												//frame is finished transmitting, let's go to the next one
				countFrameParts = 0;								//reset some values to go ahead
				for (m=0;m<numberOfDevices;m++) {
					deviceInUse[m] = 0;						
				}
				j = 0;
				k = 0;
							}
			}
			//i++;
			systemTime++;											//increment the system time
			
		}//end of the for-ever-while
		
		//count the sent frames and the network throughput
		for (i=0;i<numberOfFrames;i++) {
			if (frameCol[i]<10) {
				framesSent++;
				networkthroughput = networkthroughput + saveFrameLength[i];
			}
			else {
				framesDropt++;
			}
		}
		
		printPerformance(networkthroughput, framesDropt, networkthroughput / systemTime * 10);
		
		
	return 0;
	}
	
	// main routine: getting the external date through the terminal and calls the program
	public static void main (String args[])
	{
		int seed;
		int numberOfDevices;
		int numberOfFrames;
		double arrivalRate;
		
		//store the arguments given in the terminal to the program in local variables
		seed = Integer.parseInt(args[0]);
		numberOfDevices = Integer.parseInt(args[1]);
		numberOfFrames = Integer.parseInt(args[2]);
		arrivalRate = Double.parseDouble(args[3]);
		
		//make new instance
		Ethernet myEthernet = new Ethernet();

		//call the actual program
		myEthernet.runSimulation(seed,numberOfDevices,numberOfFrames,arrivalRate);
	}
}
