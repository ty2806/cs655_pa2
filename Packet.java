import java.util.Arrays;

public class Packet {
    private int seqnum;
    private int acknum;
    private int checksum;
    private String payload;
    private int[] sacks;

    public int[] getSacks() {
        return sacks;
    }

    public void setSacks(int[] sacks) {
        this.sacks = sacks;
    }

    public Packet(Packet p) {
        seqnum = p.getSeqnum();
        acknum = p.getAcknum();
        checksum = p.getChecksum();
        payload = new String(p.getPayload());
        sacks = p.getSacks();
    }

    public Packet(int seq, int ack, int check, String newPayload) {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        this.sacks = new int[5];
        Arrays.fill(this.sacks, -1);
        if (newPayload == null) {
            payload = "";
        } else if (newPayload.length() > NetworkSimulator.MAXDATASIZE) {
            payload = null;
        } else {
            payload = new String(newPayload);
        }
    }

    public Packet(int seq, int ack, int check, String newPayload, int[] sacks) {
        this(seq, ack, check, newPayload);
        this.sacks = sacks;
    }


    public Packet(int seq, int ack, int check) {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        payload = "";
        sacks = new int[5];
        Arrays.fill(this.sacks, -1);

    }

    public Packet(int seq, int ack, int check, int[] sacks) {
        this(seq, ack, check);
        this.sacks = sacks;
    }


    public boolean setSeqnum(int n) {
        seqnum = n;
        return true;
    }

    public boolean setAcknum(int n) {
        acknum = n;
        return true;
    }

    public boolean setChecksum(int n) {
        checksum = n;
        return true;
    }

    public boolean setPayload(String newPayload) {
        if (newPayload == null) {
            payload = "";
            return false;
        } else if (newPayload.length() > NetworkSimulator.MAXDATASIZE) {
            payload = "";
            return false;
        } else {
            payload = new String(newPayload);
            return true;
        }
    }

    public int getSeqnum() {
        return seqnum;
    }

    public int getAcknum() {
        return acknum;
    }

    public int getChecksum() {
        return checksum;
    }

    public String getPayload() {
        return payload;
    }

    public String toString() {
        return ("seqnum: " + seqnum + "  acknum: " + acknum + "  checksum: " +
                checksum + "  payload: " + payload + " sacks: " + Arrays.toString(this.sacks));
    }

}
