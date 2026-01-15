package experiments;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Random;

import zk.sharp1s.Sharp1sProof;
import zk.sharp1s.Sharp1sProver;
import zk.sharp1s.Sharp1sVerifier;

public class Sharp1sExperiments {

	Random generator;

	public BigInteger[] generateX(int N, int maxVal) {
		BigInteger[] x = new BigInteger[N];
		for(int i=0;i<N;i++) {
			x[i] = BigInteger.valueOf(generator.nextInt(maxVal));
		}
		return x;
	}

	public void run() throws NoSuchAlgorithmException {
		int N = 100;
		int R = 40;
		BigInteger L = new BigInteger("1208925819614629174706176");
		int maxVal = 1000;

		//		pc = PedersenCommitment.getDefault();
		//		generator = new Random();
		//		transcript = new Transcript();
		//		prover = new Prover(transcript, pc);

		long genTime = 0;
		long vrfTime = 0;
		long proofSize = 0;

		int NN = 1;

		generator = new Random(100001);

		for(int i=0;i<NN;i++) {
			Sharp1sProver Sharp1sProver = new Sharp1sProver();
			Sharp1sVerifier Sharp1sVerifier = new Sharp1sVerifier();

			BigInteger[] x = generateX(N, maxVal);
			BigInteger r_x = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));

			long startGen = System.currentTimeMillis();
			Sharp1sProof proof = Sharp1sProver.generateRangeProof(x, r_x, R, L);
			long endGen = System.currentTimeMillis(); 

			long startVrf = System.currentTimeMillis(); 
			boolean match = Sharp1sVerifier.verify(proof, L);
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

	public void genTable() throws NoSuchAlgorithmException {
		int[] Ns = {1, 8, 32, 128, 512};
		int[] Rs = {1, 8, 32, 128, 512};
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
					Sharp1sProver Sharp1sProver = new Sharp1sProver();
					Sharp1sVerifier Sharp1sVerifier = new Sharp1sVerifier();

					BigInteger[] x = generateX(N, maxVal);
					BigInteger r_x = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));

					long startGen = System.currentTimeMillis();
					Sharp1sProof proof = Sharp1sProver.generateRangeProof(x, r_x, R, L);
					long endGen = System.currentTimeMillis(); 

					long startVrf = System.currentTimeMillis(); 
					boolean match = Sharp1sVerifier.verify(proof, L);
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

			new Sharp1sExperiments().run();
//			new Sharp1sExperiments().genTable();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

