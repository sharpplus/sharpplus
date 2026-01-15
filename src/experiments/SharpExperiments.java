package experiments;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Random;

import util.SharpUtils;
import zk.sharp.SharpAttacker1;
import zk.sharp.SharpAttacker2;
import zk.sharp.SharpProof;
import zk.sharp.SharpProver;
import zk.sharp.SharpVerifier;

public class SharpExperiments {

	Random generator;

	public void run() throws NoSuchAlgorithmException {
		int N = 100;
		int R = 40;
		int B = 8000;
		BigInteger L = new BigInteger("1208925819614629174706176");
		int maxVal = 1000;

		//		pc = PedersenCommitment.getDefault();
		//		generator = new Random();
		//		transcript = new Transcript();
		//		prover = new Prover(transcript, pc);

		long genTime = 0;
		long vrfTime = 0;
		long proofSize = 0;

		int NN = 10;

		generator = new Random(100001);

		for(int i=0;i<NN;i++) {
			SharpProver sharpProver = new SharpProver();
			SharpVerifier sharpVerifier = new SharpVerifier();

			BigInteger[] x = SharpUtils.generateX(generator, N, maxVal);
			BigInteger r_x = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));

			long startGen = System.currentTimeMillis();
			SharpProof proof = sharpProver.generateRangeProof(x, r_x, BigInteger.valueOf(B), R, L);
			long endGen = System.currentTimeMillis(); 

			long startVrf = System.currentTimeMillis(); 
			boolean match = sharpVerifier.verify(proof, sharpProver.getCx(), BigInteger.valueOf(B), L);
			long endVrf = System.currentTimeMillis();

			proofSize += proof.size();

			genTime += endGen - startGen;
			vrfTime += endVrf - startVrf;

			System.out.println(match ? "Success" : "Fail");

			System.out.println("total generation time: "+(genTime * 1. / 1000)+"s, average: "+(genTime * 1. / 1000 / NN)+"s");
			System.out.println("total verification time: "+(vrfTime * 1. / 1000)+"s, average: "+(vrfTime * 1. / 1000 / NN)+"s");
			System.out.println("total proof size: "+(proofSize)+"B, average: "+(proofSize * 1. / NN)+"B");
		}
	}

	public void run2() throws NoSuchAlgorithmException {
		int[] Ns = new int[] {50, 100, 150, 200, 250, 300};
		int R = 40;
		int B = 8000;
		BigInteger L = new BigInteger("1208925819614629174706176");
		int maxVal = 1000;

		//		pc = PedersenCommitment.getDefault();
		//		generator = new Random();
		//		transcript = new Transcript();
		//		prover = new Prover(transcript, pc);

		long[] genTime = new long[Ns.length];
		long[] vrfTime = new long[Ns.length];
		long[] proofSize = new long[Ns.length];

		int NN = 10;

		generator = new Random(100001);
		for(int j=0;j<Ns.length;j++) {
			int N = Ns[j];
			for(int i=0;i<NN;i++) {
				SharpProver sharpProver = new SharpProver();
				SharpVerifier sharpVerifier = new SharpVerifier();

				BigInteger[] x = SharpUtils.generateX(generator, N, maxVal);
				BigInteger r_x = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));

				long startGen = System.currentTimeMillis();
				SharpProof proof = sharpProver.generateRangeProof(x, r_x, BigInteger.valueOf(B), R, L);
				long endGen = System.currentTimeMillis(); 

				long startVrf = System.currentTimeMillis(); 
				boolean match = sharpVerifier.verify(proof, sharpProver.getCx(), BigInteger.valueOf(B), L);
				long endVrf = System.currentTimeMillis();

				proofSize[j] += proof.size();
				genTime[j] += endGen - startGen;
				vrfTime[j] += endVrf - startVrf;

				System.out.println(match ? "Success" : "Fail");
			}
		}
		
		for(int j=0;j<Ns.length;j++) {
			System.out.println("N="+Ns[j]+", total generation time: "+(genTime[j] * 1. / 1000)+"s, average: "+(genTime[j] * 1. / 1000 / NN)+"s");
			System.out.println("N="+Ns[j]+", total verification time: "+(vrfTime[j] * 1. / 1000)+"s, average: "+(vrfTime[j] * 1. / 1000 / NN)+"s");
			System.out.println("N="+Ns[j]+", total proof size: "+(proofSize[j] * 1. / 1000)+"kB, average: "+(proofSize[j] * 1. / 1000 / NN)+"kB");
		}
	}

	public void runAttack() throws NoSuchAlgorithmException {
		int N = 100;
		int R = 40;
		int B = 8000;
		BigInteger L = new BigInteger("1208925819614629174706176");
		int maxVal = 1000;

		generator = new Random(100001);

		SharpAttacker1 sharpProver = new SharpAttacker1();
		SharpVerifier sharpVerifier = new SharpVerifier();

		BigInteger[] x = SharpUtils.generateX(generator, N, maxVal);

		BigInteger r_x = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));

		SharpProof proof = sharpProver.generateRangeProof(x, r_x, BigInteger.valueOf(B), R, L);

		boolean match = sharpVerifier.verify(proof, sharpProver.getCx(), BigInteger.valueOf(B), L);

		System.out.println(match ? "Success" : "Fail");
	}

	public void runAttack2() throws NoSuchAlgorithmException {
		int N = 100;
		int R = 40;
		int B = 20;
		BigInteger L = new BigInteger("1208925819614629174706176");
		int maxVal = 20;

		generator = new Random(100001);

		SharpAttacker2 sharpProver = new SharpAttacker2();
		SharpVerifier sharpVerifier = new SharpVerifier();

		BigInteger[] x = SharpUtils.generateX(generator, N, maxVal);

		x[0] = BigInteger.valueOf(-2);
		x[1] = BigInteger.valueOf(17);
		x[2] = BigInteger.valueOf(18);
		x[3] = BigInteger.valueOf(19);
		x[4] = BigInteger.valueOf(20);

		BigInteger r_x = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));

		SharpProof proof = sharpProver.generateRangeProof(x, r_x, BigInteger.valueOf(B), R, L);

		boolean match = sharpVerifier.verify(proof, sharpProver.getCx(), BigInteger.valueOf(B), L);

		System.out.println(match ? "Success" : "Fail");
	}

	public void genTable() throws NoSuchAlgorithmException {
		int[] Ns = {1, 8, 32, 128, 512};
		int[] Rs = {1, 8, 32, 128, 512};
		int B = 8000;
		BigInteger L = new BigInteger("1208925819614629174706176");
		int maxVal = 1000;

		//		pc = PedersenCommitment.getDefault();
		//		generator = new Random();
		//		transcript = new Transcript();
		//		prover = new Prover(transcript, pc);



		int NN = 100;

		generator = new Random(100001);

		String genText = "";
		String vrfText = "";
		String sizText = "";

		for(int ni=0;ni<Ns.length;ni++) {
			for(int nr=0;nr<Rs.length;nr++) {
				int N = Ns[ni];
				int R = Rs[nr];

				long genTime = 0;
				long vrfTime = 0;
				long proofSize = 0;

				for(int i=0;i<NN;i++) {
					SharpProver sharpProver = new SharpProver();
					SharpVerifier sharpVerifier = new SharpVerifier();

					BigInteger[] x = SharpUtils.generateX(generator, N, maxVal);
					BigInteger r_x = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));

					long startGen = System.currentTimeMillis();
					SharpProof proof = sharpProver.generateRangeProof(x, r_x, BigInteger.valueOf(B), R, L);
					long endGen = System.currentTimeMillis(); 

					long startVrf = System.currentTimeMillis(); 
					boolean match = sharpVerifier.verify(proof, sharpProver.getCx(), BigInteger.valueOf(B), L);
					long endVrf = System.currentTimeMillis();

					proofSize += proof.size();

					genTime += endGen - startGen;
					vrfTime += endVrf - startVrf;

					System.out.println(match ? "Success" : "Fail");
				}

				DecimalFormat df1 = new DecimalFormat("#.###");
				DecimalFormat df2 = new DecimalFormat("#");

				genText += "N="+N+"\t R="+R+"\t gen (s) "+df1.format(genTime * 1. / 1000 / NN)+"\n";
				vrfText += "N="+N+"\t R="+R+"\t vrf (s) "+df1.format(vrfTime * 1. / 1000 / NN)+"\n";
				sizText += "N="+N+"\t R="+R+"\t siz () "+df2.format(proofSize * 1. / NN)+"\n";
			}
		}

		System.out.println(genText);
		System.out.println(vrfText);
		System.out.println(sizText);
	}

	public static void main(String[] a) {
		try {

			//			new SharpExperiments().run();
			new SharpExperiments().run2();
			//			new SharpExperiments().runAttack();
			//			new SharpExperiments().runAttack2();
			//			new SharpExperiments().genTable();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

