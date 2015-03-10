import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

class Generator {

	static Scanner scan = new Scanner(System.in);
	static FileOutputStream fos = null;
	static DataOutputStream dos = null;
	static FileInputStream fis = null;
	static DataInputStream dis = null;
	static double tick = .025; // calculation period in s

	public static void main(String[] args) {
		int type = 0;

		System.out.println("input name for new command");
		String name = scan.next();

		try {
			fos = new FileOutputStream(name + ".txt", true);
			dos = new DataOutputStream(fos);
			fis = new FileInputStream(name + ".txt");
			dis = new DataInputStream(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("type 1 for rot, 2 for drv");
		type = scan.nextInt();

		double distance = 0;
		if (type == 1)
			distance = dRot();
		else if (type == 2)
			distance = dDrv();
		else {
			System.out.println("lol im leavin");
			return;
		}

		System.out.println("maximum accel (in/s^2)");
		double accelMax = scan.nextDouble();

		System.out.println("cruise velocity (in/s)");
		double cruiseVelocity = scan.nextDouble();

		System.out.println("calculating...\n\n");
		System.out.println("\tdistance = " + distance);
		calculate(distance, accelMax, cruiseVelocity);

		try {
			fos.close();
			dos.close();
			fis.close();
			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static double dRot() {
		System.out.println("type angle to rotate counter clockwise (deg)");
		double theta = scan.nextDouble();
		System.out.println("drivetrain width (in)");
		double w = scan.nextDouble();
		System.out.println("drivetrain length (in)");
		double l = scan.nextDouble();

		return theta * Math.PI / 180.0
				* Math.sqrt(Math.pow(w, 2) + Math.pow(l, 2));
	}

	private static double dDrv() {
		System.out.println("distance to strafe (right is +) (in)");
		double str = scan.nextDouble();
		System.out.println("distance to forward (forward is +) (in)");
		double fwd = scan.nextDouble();

		try {
			dos.writeDouble(fwd);
			// dos.writeChars("\n");
			dos.writeDouble(str);
			// dos.writeChars("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Math.sqrt(Math.pow(str, 2) + Math.pow(fwd, 2));
	}

	private static void calculate(double distance, double accelMax,
			double cruiseVelocity) {

		ArrayList<Double> vel = new ArrayList<Double>();
		vel.add(0.0);
		ArrayList<Double> pos = new ArrayList<Double>();
		pos.add(0.0);

		double t = 0;

		final double n = (distance / cruiseVelocity) / tick;
		final double ramptime = cruiseVelocity / accelMax;

		ArrayList<Double> filter1 = new ArrayList<Double>();
		filter1.add(0.0);
		ArrayList<Double> filter2 = new ArrayList<Double>();
		filter2.add(0.0);
		final int filterconst = (int) Math.round(ramptime / tick + .5);

		for (int step = 1; Math.abs(pos.get(step - 1)) + 0.1 < Math
				.abs(distance); step++) {

			t += (step - 1) * tick;
			int in = (step < (n + 2) ? 1 : 0);
			filter1.add(Math.max(0, Math.min(1, filter1.get(step - 1)
					+ (in == 1 ? 1.0 / filterconst : -1.0 / filterconst))));

			double sum = 0;
			for (int step2 = -1 * Math.min(filterconst, step); step2 + step < 1; step2++) {
				sum += filter1.get(step2 + step);
			}
			filter2.add(sum);

			vel.add(10 * filter1.get(step) + filter2.get(step) / (1 + filterconst)
					* cruiseVelocity);
			pos.add(10* (vel.get(step) + vel.get(step - 1)) / 2 * tick
					+ pos.get(step - 1));

			try {
				System.out.println("\t" + vel.get(step) + "\t" + pos.get(step)
						+ "\t||\t" + dis.readDouble());
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			try {
				dos.writeDouble(vel.get(step));
				// dos.writeChars("\n");
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		System.out.println("... successfully");
	}
}