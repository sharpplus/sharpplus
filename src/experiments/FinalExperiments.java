package experiments;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Random;

import ec.Scalar;
import util.SharpUtils;
import zk.bulletproofs.BpProof;
import zk.bulletproofs.BulletProofGenerators;
import zk.bulletproofs.PedersenCommitment;
import zk.bulletproofs.Utils;
import zk.sharp.SharpProof;
import zk.sharp.SharpProver;
import zk.sharp.SharpVerifier;
import zk.sharp1s.Sharp1sProof;
import zk.sharp1s.Sharp1sProver;
import zk.sharp1s.Sharp1sVerifier;
import zk.sharpmulti.SharpMultiProof;
import zk.sharpmulti.SharpMultiProver;
import zk.sharpmulti.SharpMultiVerifier;
import zk.sharpplus.SharpPlusProof;
import zk.sharpplus.SharpPlusProver;
import zk.sharpplus.SharpPlusVerifier;

public class FinalExperiments {
	
	String ci(DecimalFormat dfZero, double v, double w) {
		if(!(w>0)) return "\\confintv{"+dfZero.format(v)+"}{"+dfZero.format(0)+"}";
		return 	"\\confintv{"+dfZero.format(v)+"}{"+dfZero.format(w)+"}";
	}

	void printAsScalabilityTable(double[][] results, double[][] ciwidths, int[] Rs, int[] Ns, boolean ci) {
		DecimalFormat dfZero = new DecimalFormat("0.0");

		for(int Ni=0;Ni<Ns.length;Ni++) {
			System.out.print(" & "+Ns[Ni]);
		}
		System.out.println(" \\\\ \\toprule");

		for(int Ri=0;Ri<Rs.length;Ri++) {
			System.out.print(Rs[Ri]);
			for(int Ni=0;Ni<Ns.length;Ni++) {
				if(ci) System.out.print(" & "+ci(dfZero, results[Ri][Ni], ciwidths[Ri][Ni]));
				else System.out.print(" & "+dfZero.format(results[Ri][Ni]));
			}
			System.out.println(" \\\\");
		}
	}

	public BigInteger[] generateX(int N, int maxVal, Random generator) {
		BigInteger[] x = new BigInteger[N];
		for(int i=0;i<N;i++) {
			x[i] = BigInteger.valueOf(generator.nextInt(maxVal));
		}
		return x;
	}

	public void generateScalabilityTables(int[] Rs, int[] Ns, int NN) throws IOException, NoSuchAlgorithmException {

		double[][] prvTimeResults = new double[Rs.length][Ns.length];
		double[][] vrfTimeResults = new double[Rs.length][Ns.length];
		double[][] sizeResults = new double[Rs.length][Ns.length];

		double[][] prvTimeResults2 = new double[Rs.length][Ns.length];
		double[][] vrfTimeResults2 = new double[Rs.length][Ns.length];

		for(int Ri = 0; Ri < Rs.length; Ri++) {
			int R = Rs[Ri];

			BufferedWriter writer = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/output/figure1_R_"+R+"_NN_"+NN+".csv"));

			System.out.println("-----");
			String ss = "N,avgGenTime,sdGenTime";
			writer.write(ss+"\n");
			System.out.println(ss);

			for(int Ni = 0; Ni < Ns.length; Ni++) {
				int N = Ns[Ni];
				int M = Ns[Ni];

				long totGen = 0;
				long totVrf = 0;
				long totSize = 0;
				long totGen2 = 0;
				long totVrf2 = 0;
				long totSize2 = 0;

				Random generator = new Random(100001);
				int maxVal = 5000;
				BigInteger L = new BigInteger("1208925819614629174706176");

				for(int NNi = 0; NNi < NN; NNi++) {
					SharpPlusProver sharpComboProver = new SharpPlusProver();
					SharpPlusVerifier sharpComboVerifier = new SharpPlusVerifier();

					BigInteger[] x = generateX(N, maxVal, generator);
					BigInteger[] x_prime_1 = generateX(M, maxVal, generator);
					BigInteger[] x_prime_2 = generateX(M, maxVal, generator);
					
					BigInteger[] r_x = new BigInteger[N];
					for(int i=0;i<N;i++) {
						r_x[i] = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));
					}
					
					BigInteger[] r_x_prime1 = new BigInteger[N];
					BigInteger[] r_x_prime2 = new BigInteger[N];
					for(int i=0;i<M;i++) {
						r_x_prime1[i] = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));
						r_x_prime2[i] = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));
					}
					BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));

					long startGen = System.currentTimeMillis();
					SharpPlusProof proof = sharpComboProver.generateComboProof(x, x_prime_1, x_prime_2, r_x, r_x_prime1, r_x_prime2, R, L);
					long endGen = System.currentTimeMillis(); 

					long startVrf = System.currentTimeMillis(); 
					boolean match = sharpComboVerifier.verify(proof, sharpComboProver.getCx(), sharpComboProver.getCxprime1(), sharpComboProver.getCxprime2(), L);
					if(!match) System.out.println("Error: proof failed");
					long endVrf = System.currentTimeMillis();

					totGen += endGen - startGen;
					totVrf += endVrf - startVrf;
					totSize += proof.size();

					totGen2 += (endGen - startGen)*(endGen - startGen);
					totVrf2 += (endVrf - startVrf)*(endVrf - startVrf);
					totSize2 += proof.size()*proof.size();
				}

				System.out.println((totSize2/(NN-1) - 1.*totSize*totSize/NN/NN));

				String s = N+" "+(1.*totGen/NN)+","
						+Math.sqrt((totGen2/(NN-1) - 1.*totGen*totGen/NN/NN)/NN)+","
						+(1.*totVrf/NN)+","
						+Math.sqrt((totVrf2/(NN-1) - 1.*totVrf*totVrf/NN/NN)/NN)+","
						+(1.*totSize/NN)+","
						+Math.sqrt((totSize2/(NN-1) - 1.*totSize*totSize/NN/NN)/NN);

				System.out.println(s);
				writer.write(s+"\n");

				prvTimeResults[Ri][Ni] = 1.*totGen/NN;
				vrfTimeResults[Ri][Ni] = 1.*totVrf/NN;
				sizeResults[Ri][Ni] = 1.*totSize/NN / 1000;
				
				prvTimeResults2[Ri][Ni] = 1.96 * Math.sqrt((totGen2/(NN-1) - 1.*totGen*totGen/NN/NN)/NN);
				vrfTimeResults2[Ri][Ni] = 1.96 * Math.sqrt((totVrf2/(NN-1) - 1.*totVrf*totVrf/NN/NN)/NN);
			}

			writer.close();
		}

		printAsScalabilityTable(prvTimeResults, prvTimeResults2, Rs, Ns, true);
		printAsScalabilityTable(vrfTimeResults, vrfTimeResults2, Rs, Ns, true);
		printAsScalabilityTable(sizeResults, null, Rs, Ns, false);
	}

	void printAsComparisonTable(double[][] results, double[][] ciwidths, String[] methods, int[] Ns, boolean ci) {
		DecimalFormat dfZero = new DecimalFormat("0.0");

		for(int Ni=0;Ni<Ns.length;Ni++) {
			System.out.print(" & "+Ns[Ni]);
		}
		System.out.println(" \\\\ \\toprule");

		for(int Ri=0;Ri<methods.length;Ri++) {
			System.out.print(methods[Ri]);
			for(int Ni=0;Ni<Ns.length;Ni++) {
				if(ci) System.out.print(" & "+ci(dfZero, results[Ri][Ni], ciwidths[Ri][Ni]));
				else System.out.print(" & "+dfZero.format(results[Ri][Ni]));
			}
			System.out.println(" \\\\");
		}
	}

	public void generateComparisonTables(int[] Ns, int NN) throws IOException, NoSuchAlgorithmException {

		double[][] prvTimeResults = new double[5][Ns.length];
		double[][] vrfTimeResults = new double[5][Ns.length];
		double[][] sizeResults = new double[5][Ns.length];
		
		double[][] prvTimeResults2 = new double[5][Ns.length];
		double[][] vrfTimeResults2 = new double[5][Ns.length];

		int R = 256;

		for(int Ni = 0; Ni < Ns.length; Ni++) {
			int N = Ns[Ni];
			int M = Ns[Ni];

			long[] totGen = new long[5];
			long[] totVrf = new long[5];
			long[] totSize = new long[5];
			long[] totGen2 = new long[5];
			long[] totVrf2 = new long[5];
			long[] totSize2 = new long[5];

			Random generator = new Random(100001);
			int maxVal = 5000;
			int B = 10000;
			BigInteger L = new BigInteger("1208925819614629174706176");

			for(int NNi = 0; NNi < NN; NNi++) {
				SharpProver sharpProver = new SharpProver();
				SharpVerifier sharpVerifier = new SharpVerifier();
				Sharp1sProver sharp1sProver = new Sharp1sProver();
				Sharp1sVerifier sharp1sVerifier = new Sharp1sVerifier();
				SharpMultiProver sharpMultiProver = new SharpMultiProver();
				SharpMultiVerifier sharpMultiVerifier = new SharpMultiVerifier();
				SharpPlusProver sharpComboProver = new SharpPlusProver();
				SharpPlusVerifier sharpComboVerifier = new SharpPlusVerifier();

				BigInteger[] x = SharpUtils.generateX(generator, N, maxVal);
				BigInteger r_x = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));
				
				BigInteger[] r_xx = new BigInteger[N];
				for(int i=0;i<N;i++) {
					r_xx[i] = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));
				}
				
				BigInteger[] r_x_prime1 = new BigInteger[N];
				BigInteger[] r_x_prime2 = new BigInteger[N];
				for(int i=0;i<M;i++) {
					r_x_prime1[i] = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));
					r_x_prime2[i] = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));
				}

				BigInteger[] x_prime_1 = generateX(M, maxVal, generator);
				BigInteger[] x_prime_2 = generateX(M, maxVal, generator);
				BigInteger[] y_prime = new BigInteger[M];

				for(int i=0;i<M;i++) {
					y_prime[i] = x_prime_1[i].multiply(x_prime_2[i]);
				}
				
				long startGen, endGen, startVrf, endVrf;
				boolean match;
				
				// bulletproofs
				
				BigInteger value = BigInteger.valueOf(12L);
				
				long min = 10;
				long max = 100;
				int bitsize = 31;
				
				PedersenCommitment pc = PedersenCommitment.getDefault();
				BulletProofGenerators bg1 = new BulletProofGenerators(128, 1);

				Scalar rnd = Utils.randomScalar();

				startGen = System.currentTimeMillis();
				BpProof BpProof = BulletproofTests.generateRangeProof(value, min, max, bitsize, rnd, pc, bg1);
				endGen = System.currentTimeMillis();

				BulletProofGenerators bg2 = new BulletProofGenerators(128, 1);

				startVrf = System.currentTimeMillis();
				match = BulletproofTests.verifyRangeProof(min, max, bitsize, BpProof, pc, bg2);
				endVrf = System.currentTimeMillis();
				
				totGen[0] += (endGen - startGen) * N;
				totVrf[0] += (endVrf - startVrf) * N;
				totSize[0] += (BpProof.size()) * N;
				
				totGen2[0] += (endGen - startGen)*(endGen - startGen) * N * N;
				totVrf2[0] += (endVrf - startVrf)*(endVrf - startVrf) * N * N;
				totSize2[0] += BpProof.size() * BpProof.size() * N * N;
				
				// sharp

				startGen = System.currentTimeMillis();
				SharpProof proof = sharpProver.generateRangeProof(x, r_x, BigInteger.valueOf(B), R, L);
				endGen = System.currentTimeMillis(); 

				startVrf = System.currentTimeMillis(); 
				match = sharpVerifier.verify(proof, sharpProver.getCx(), BigInteger.valueOf(B), L);
				if(!match) System.out.println("Error: proof failed");
				endVrf = System.currentTimeMillis();

				totGen[1] += endGen - startGen;
				totVrf[1] += endVrf - startVrf;
				totSize[1] += proof.size();

				totGen2[1] += (endGen - startGen)*(endGen - startGen);
				totVrf2[1] += (endVrf - startVrf)*(endVrf - startVrf);
				totSize2[1] += proof.size()*proof.size();
				
				// sharp one-sided

				startGen = System.currentTimeMillis();
				Sharp1sProof oneProof = sharp1sProver.generateRangeProof(x, r_x, R, L);
				endGen = System.currentTimeMillis(); 

				startVrf = System.currentTimeMillis(); 
				match = sharp1sVerifier.verify(oneProof, L);
				if(!match) System.out.println("Error: proof failed");
				endVrf = System.currentTimeMillis();

				totGen[2] += endGen - startGen;
				totVrf[2] += endVrf - startVrf;
				totSize[2] += oneProof.size();

				totGen2[2] += (endGen - startGen)*(endGen - startGen);
				totVrf2[2] += (endVrf - startVrf)*(endVrf - startVrf);
				totSize2[2] += oneProof.size()*oneProof.size();
				
				// sharp multi

				startGen = System.currentTimeMillis();
				SharpMultiProof multiProof = sharpMultiProver.generateMultiProof(y_prime, x_prime_1, x_prime_2, r_x);
				endGen = System.currentTimeMillis(); 

				startVrf = System.currentTimeMillis(); 
				match = sharpMultiVerifier.verify(multiProof);
				if(!match) System.out.println("Error: proof failed");
				endVrf = System.currentTimeMillis();

				totGen[3] += endGen - startGen;
				totVrf[3] += endVrf - startVrf;
				totSize[3] += multiProof.size();

				totGen2[3] += (endGen - startGen)*(endGen - startGen);
				totVrf2[3] += (endVrf - startVrf)*(endVrf - startVrf);
				totSize2[3] += multiProof.size()*multiProof.size();
				
				// combo

				startGen = System.currentTimeMillis();
				SharpPlusProof comboProof = sharpComboProver.generateComboProof(x, x_prime_1, x_prime_2, r_xx, r_x_prime1, r_x_prime2, R, L);
				endGen = System.currentTimeMillis(); 

				startVrf = System.currentTimeMillis(); 
				match = sharpComboVerifier.verify(comboProof, sharpComboProver.getCx(), sharpComboProver.getCxprime1(), sharpComboProver.getCxprime2(), L);
				if(!match) System.out.println("Error: proof failed");
				endVrf = System.currentTimeMillis();

				totGen[4] += endGen - startGen;
				totVrf[4] += endVrf - startVrf;
				totSize[4] += comboProof.size();

				totGen2[4] += (endGen - startGen)*(endGen - startGen);
				totVrf2[4] += (endVrf - startVrf)*(endVrf - startVrf);
				totSize2[4] += comboProof.size()*comboProof.size();
			}
			
			System.out.println("finished N = "+N);

			for(int i=0;i<prvTimeResults.length;i++) {
				prvTimeResults[i][Ni] = 1.*totGen[i]/NN;
				vrfTimeResults[i][Ni] = 1.*totVrf[i]/NN;
				sizeResults[i][Ni] = 1.*totSize[i]/NN / 1000;
				
				prvTimeResults2[i][Ni] = 1.96 * Math.sqrt((totGen2[i]/(NN-1) - 1.*totGen[i]*totGen[i]/NN/NN)/NN);
				vrfTimeResults2[i][Ni] = 1.96 * Math.sqrt((totVrf2[i]/(NN-1) - 1.*totVrf[i]*totVrf[i]/NN/NN)/NN);
			}
		}
		String[] methods = new String[] {"Bulletproofs", "\\sharp{}", "\\sharp{}$_{\\geq 0}$", "\\sharp{}$_{\\times}$", "\\name{}"};

		printAsComparisonTable(prvTimeResults, prvTimeResults2, methods, Ns, true);
		printAsComparisonTable(vrfTimeResults, vrfTimeResults2, methods, Ns, true);
		printAsComparisonTable(sizeResults, null, methods, Ns, false);
	}

	public void run() {
		try {
			int N = 100;
			generateComparisonTables(new int[] {1, 4, 16, 64, 256, 1024}, N); 
			generateScalabilityTables(new int[] {32, 64, 128, 256, 512, 1024}, new int[] {1, 4, 16, 64, 256, 1024}, N);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] a) {
		new FinalExperiments().run();
	}	

}
