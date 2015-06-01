import java.io.*;
import java.util.Arrays;

public class parse {

	public static void main(String[] args) {
	byte received [] = {0, 1, 116, 101, 115, 116, 49, 46, 116, 120, 116, 0, 111, 99, 116, 101, 116, 0};
	byte[] errMode = new byte[0];
	String newMode = "wrongMode!";
	ByteArrayOutputStream rec = new ByteArrayOutputStream();
	try {
		rec.write(newMode.getBytes("US-ASCII"));
		rec.write(0);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	errMode = rec.toByteArray();
	System.out.println(Arrays.toString(received));
	System.out.println(Arrays.toString(errMode));
	for (int i=received[1];i<=received.length-2;i++){
		if (received[i] == 0){
			byte[] errRec = new byte[i+errMode.length+1];
			System.arraycopy(received, 0, errRec, 0, i);
			System.out.println(Arrays.toString(errRec));
			System.arraycopy(errMode, 0, errRec, i+1, errMode.length);
			System.out.println(Arrays.toString(errRec));
		}
	}
}
}