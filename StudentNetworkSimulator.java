import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator {
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity):
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment):
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData):
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // These constants represent the meaning of acknowledgement number in a packet
    // 0 represent the packet contains data
    // 1 represent the packet contains ack
    public static final int AckNumData = 0;

    public static final int AckNumAck = 1;

    // Sender state variable
    // Send window size
    private int SWS;

    // Last acknowledgment received
    private int LAR;

    // Last packet sent
    private int LPS;

    // Sender buffer
    private ArrayList<Packet> SenderBuffer;

    // Sender buffer size
    public static final int SenderBufferSize = 50;

    // Receiver state variable
    // Receive window size
    private int RWS;

    // Last packet acceptable
    private int LPA;

    // Next packet expected
    private int NPE;

    // Receiver buffer
    private Queue<Packet> receiverBuffer;

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay) {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = winsize * 2; // set appropriately; assumes SR here!
        RxmtInterval = delay;
    }

    // Calculate the difference of 2 number module sequence number
    protected int calculateDiff(int low, int high) {
        int diff = low - high;
        if (diff < 0) {
            diff += LimitSeqNo;
        }
        return diff;
    }

    // Calculate the checksum of a string
    protected int calculateCheckSum(String data) {
        int hash = data.hashCode();
        return hash;
    }

    // check if a given packet has corrupted
    // return false if the packet is corrupted
    // otherwise return true
    protected boolean checkCorruption(Packet p) {
        int checksum = p.getChecksum();
        String data = p.getSeqnum() + p.getAcknum() + p.getPayload();

        if (checksum == calculateCheckSum(data))
            return true;
        else return false;
    }

    // possible alternative for checksum in checkCorruption method
    //  We would suggest a TCP-like
    //  checksum, which consists of the sum of the (integer) sequence and ack field values, added to a
    //  character-by-character sum of the payload field of the packet (i.e., treat each character as if it
    //  were an 8-bit integer and just add them together).
    protected int generateChecksum(int seqnum, int acknum, String payload) {
        int checksum = 0;
        checksum += seqnum;
        checksum += acknum;
        for (int i = 0; i < payload.length(); i++) {
            checksum += (byte) payload.charAt(i);
        }
        return checksum;
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to ensure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message) {
        // drop new message when buffer is full
        if (SenderBuffer.size() >= SenderBufferSize) {
            if (traceLevel >= 2) {
                System.out.println("Sender buffer size(" + SenderBufferSize + ") is full. New message from layer 5 is dropped.");
            }
            return;
        }

        String payload = message.getData();
        int acknum = AckNumData;

        int lastSeqNum = SenderBuffer.get(SenderBuffer.size() - 1).getSeqnum();
        int seqnum = (lastSeqNum + 1) % LimitSeqNo;

        int checksum = calculateCheckSum(seqnum + acknum + payload);
        Packet packet = new Packet(seqnum, acknum, checksum, payload);
        SenderBuffer.add(packet);

        if (calculateDiff(LAR, LPS) < SWS) {
            aSend(packet);
            LPS = (LPS + 1) % LimitSeqNo;
        }
    }

    protected void aSend(Packet p) {
        // restart timer every time a packet is pushed to layer 3
        stopTimer(A);
        toLayer3(A, p);
        startTimer(A, RxmtInterval);
    }

    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet) {
        // check if packet is corrupted
        if (checkCorruption(packet)) {
            return;
        }

        int ack = packet.getSeqnum();
        // duplicate ack
        if (ack == LAR) {
            aSend(SenderBuffer.get(0));
        }
        // new ack
        else {
            int diff = calculateDiff(LAR, ack);
            // update LAR
            LAR = ack;

            // remove acknowledged packets in buffer
            for (int i = 0; i < diff; i++) {
                SenderBuffer.remove(0);
            }

            // get the buffer index of LPS packet
            int LPSindex = 0;
            for (int i = 0; i < SenderBuffer.size(); i++) {
                if (SenderBuffer.get(i).getSeqnum() == LPS) {
                    LPSindex = i;
                    break;
                }
            }

            // send new packets starting from LPS+1
            for (int i = LPSindex + 1; i < Math.min(SenderBuffer.size(), LPSindex + diff + 1); i++) {
                aSend(SenderBuffer.get(i));
                LPS = (LPS + 1) % LimitSeqNo;
            }

        }
    }

    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt() {
        aSend(SenderBuffer.get(0));
    }

    // This routine will be called once, before any of your other A-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit() {
        SWS = WindowSize;
        LAR = -1;
        LPS = -1;
        SenderBuffer = new ArrayList<>();
    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    // FSM:
    // if corrupted packet drop it
    // if not in RWS drop
    // if is NPE, find current cumulative ack, call to layer5() send data update NPE and LPA
    // if out of order, put packet into buffer
    protected void bInput(Packet packet) {
        int acknum = packet.getAcknum();
        int seqnum = packet.getSeqnum();
        int checksum = packet.getChecksum();
        String payload = packet.getPayload();
        // if packet corrupted, drop it.
        if (!checkCorruption(packet)) {
            return;
        }

        if (!checkRWS(NPE, LPA, seqnum)) {
            return;
        }

        if (seqnum == NPE) {
            // set variable ack number to NPE, update NPE and LPA to NPE+1 and LPA+1, send to layer 5
            // while queue has item:
            // peek out the first item in the buffer
            // if the first item is NPE, poll the first item, update variable to NPE, update NPE and LPA to NPE+1 and LPA+1, send to layer 5
            // send ack number
            toLayer5(payload);
            int bAcknum = NPE;
            NPE = updateWindow(NPE, RWS);
            LPA = updateWindow(LPA, RWS);
            while (!receiverBuffer.isEmpty()) {
                Packet next = receiverBuffer.peek();
                if (next.getSeqnum() == NPE) {
                    toLayer5(next.getPayload());
                    bAcknum = NPE;
                    NPE = updateWindow(NPE, RWS);
                    LPA = updateWindow(LPA, RWS);
                    receiverBuffer.poll();
                }
            }

            // what should I send to b for seq, checksum and payload?
            // I think it doesn't matter?
            Packet ackPacket = new Packet(bAcknum, AckNumAck, generateChecksum(bAcknum, AckNumAck, ""), "");
            toLayer3(B, ackPacket);


        }
        // out of order packet received.
        else {
            receiverBuffer.add(packet);
        }

    }

    // check if current packet seqnumber is in the window
    // return true if it is in the window and false if it is not
    protected boolean checkRWS(int NPE, int LPA, int currentSequenceNumber) {
        return NPE <= currentSequenceNumber && currentSequenceNumber <= LPA ? true : false;
    }

    protected int updateWindow(int NPEorLPA, int windowSize) {
        return (NPEorLPA + 1) % windowSize;
    }

    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit() {
        RWS = WindowSize; // receive window size
        LPA = WindowSize - 1; // last packet acceptable
        NPE = 0; // next packet expected
        receiverBuffer = new PriorityQueue<>((a, b) -> {
            int seqNumA = a.getSeqnum();
            int seqNumB = b.getSeqnum();
            if (seqNumA < NPE && seqNumB < NPE) {
                return seqNumA - seqNumB;
            } else if (seqNumA < NPE) {
                return seqNumB - seqNumA;
            } else if (seqNumB < NPE) {
                return seqNumA - seqNumB;
            } else {
                return seqNumA - seqNumB;
            }
        });
    }

    // Use to print final statistics
    protected void Simulation_done() {
        // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
        System.out.println("\n\n===============STATISTICS=======================");
        System.out.println("Number of original packets transmitted by A:" + "<YourVariableHere>");
        System.out.println("Number of retransmissions by A:" + "<YourVariableHere>");
        System.out.println("Number of data packets delivered to layer 5 at B:" + "<YourVariableHere>");
        System.out.println("Number of ACK packets sent by B:" + "<YourVariableHere>");
        System.out.println("Number of corrupted packets:" + "<YourVariableHere>");
        System.out.println("Ratio of lost packets:" + "<YourVariableHere>");
        System.out.println("Ratio of corrupted packets:" + "<YourVariableHere>");
        System.out.println("Average RTT:" + "<YourVariableHere>");
        System.out.println("Average communication time:" + "<YourVariableHere>");
        System.out.println("==================================================");

        // PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
        System.out.println("\nEXTRA:");
        // EXAMPLE GIVEN BELOW
        //System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>");
    }

}
