package experiments;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ec.ECPoint;
import ec.Scalar;
import zk.bulletproofs.BpProver;
import zk.bulletproofs.BpVerifier;
import zk.bulletproofs.BulletProofGenerators;
import zk.bulletproofs.BulletProofs;
import zk.bulletproofs.Commitment;
import zk.bulletproofs.ConstraintSystem;
import zk.bulletproofs.LRO;
import zk.bulletproofs.LinearCombination;
import zk.bulletproofs.Allocated;
import zk.bulletproofs.BpProof;
import zk.bulletproofs.PedersenCommitment;
import zk.bulletproofs.Term;
import zk.bulletproofs.Transcript;
import zk.bulletproofs.Utils;
import zk.bulletproofs.Variable;

public class BulletproofTests {
	
	Random generator;

	public void testBulletBpProofSingle() {
		
	}
	
	public static BpProof generateRangeProof(BigInteger value, long min, long max, int bitsize, Scalar rnd, PedersenCommitment pedersenCommitment, BulletProofGenerators generators) {
		BigInteger a = value.subtract(BigInteger.valueOf(min));
		BigInteger b = BigInteger.valueOf(max).subtract(value);

		List<ECPoint> commitments = new ArrayList<>();

		Transcript transcript = new Transcript();
		BpProver BpProver = new BpProver(transcript, pedersenCommitment);

		Commitment vComm = BpProver.commit(Utils.scalar(value), rnd != null ? rnd : Utils.randomScalar());
		Allocated av = new Allocated(vComm.getVariable(), value);
		commitments.add(vComm.getCommitment());

		Commitment aComm = BpProver.commit(Utils.scalar(a), Utils.randomScalar());
		Allocated aa = new Allocated(aComm.getVariable(), a);
		commitments.add(aComm.getCommitment());

		Commitment bComm = BpProver.commit(Utils.scalar(b), Utils.randomScalar());
		Allocated ab = new Allocated(bComm.getVariable(), b);
		commitments.add(bComm.getCommitment());

		if (checkBound(BpProver, av, aa, ab, min, max, bitsize)) {
			return new BpProof(BpProver.prove(generators), commitments);
		} else {
			return null;
		}
	}

	public static boolean verifyRangeProof(long min, long max, int bitsize, BpProof BpProof, PedersenCommitment pedersenCommitment, BulletProofGenerators generators) {
		Transcript transcript = new Transcript();
		BpVerifier BpVerifier = new BpVerifier(transcript);

		Variable v = BpVerifier.commit(BpProof.getCommitment(0));
		Allocated av = new Allocated(v, null);

		Variable a = BpVerifier.commit(BpProof.getCommitment(1));
		Allocated aa = new Allocated(a, null);

		Variable b = BpVerifier.commit(BpProof.getCommitment(2));
		Allocated ab = new Allocated(b, null);

		if (checkBound(BpVerifier, av, aa, ab, min, max, bitsize)) {
			return BpVerifier.verify(BpProof, pedersenCommitment, generators);
		} else {
			return false;
		}
	}

	public static boolean checkBound(ConstraintSystem cs, Allocated v, Allocated a, Allocated b, long min, long max, Integer bitsize) {
		cs.constrain(LinearCombination.from(v.getVariable()).sub(LinearCombination.from(Utils.scalar(min))).sub(LinearCombination.from(a.getVariable())));
		cs.constrain(LinearCombination.from(Utils.scalar(max)).sub(LinearCombination.from(v.getVariable())).sub(LinearCombination.from(b.getVariable())));

		cs.constrainLCWithScalar(LinearCombination.from(a.getVariable()).add(LinearCombination.from(b.getVariable())), Utils.scalar(max - min));

		return verifyIsPositive(cs, a, bitsize) && verifyIsPositive(cs, b, bitsize);
	}

	public static boolean verifyIsPositive(ConstraintSystem cs, Allocated variable, int bitsize) {
		List<Term> constraints = new ArrayList<>();

		constraints.add(new Term(variable.getVariable(), BulletProofs.getFactory().minus_one()));

		Scalar exp2 = BulletProofs.getFactory().one();
		for (int i = 0; i < bitsize; i++) {
			BigInteger v = variable.getAssignment();
			BigInteger v0 = BigInteger.valueOf(0L);
			BigInteger v1 = BigInteger.valueOf(1L);
			BigInteger bit = ((variable.getAssignment() != null ? v : v0).shiftRight(i)).and(v1);
			LRO lro = cs.allocateMultiplier(Utils.scalar(v1.subtract(bit)), Utils.scalar(bit));

			// Enforce a * b = 0, so one of (a,b) is zero
			cs.constrain(LinearCombination.from(lro.getOutput()));

			// Enforce that a = 1 - b, so they both are 1 or 0
			cs.constrain(LinearCombination.from(lro.getLeft()).add(LinearCombination.from(lro.getRight()).sub(LinearCombination.from(BulletProofs.getFactory().one()))));

			constraints.add(new Term(lro.getRight(), exp2));
			exp2 = exp2.add(exp2);
		}

		// Enforce that -v + Sum(b_i * 2^i, i = 0..n-1) = 0 => Sum(b_i * 2^i, i = 0..n-1) = v
		LinearCombination lc = null;
		for (Term t : constraints) {
			lc = lc == null ? LinearCombination.from(t) : lc.add(LinearCombination.from(t));
		}
		cs.constrain(lc);

		return true;
	}

	public void run() {
		try {
			BigInteger value = BigInteger.valueOf(12L);

			long min = 10;
			long max = 100;
			int bitsize = 31;

			int[] Ns = new int[] {50, 100, 150, 200, 250, 300};

			long[] genTime = new long[Ns.length];
			long[] vrfTime = new long[Ns.length];
			long[] proofSize = new long[Ns.length];

			int NN = 1;

			for(int j=0;j<Ns.length;j++) {
				int N = Ns[j];

				for(int i=0;i<NN*N;i++) {
					PedersenCommitment pc = PedersenCommitment.getDefault();
					BulletProofGenerators bg1 = new BulletProofGenerators(128, 1);

					Scalar rnd = Utils.randomScalar();

					long startGen = System.currentTimeMillis();
					BpProof BpProof = generateRangeProof(value, min, max, bitsize, rnd, pc, bg1);
					long endGen = System.currentTimeMillis();

//					BpProof BpProof2 = BpProof.deserialize(BpProof.serialize());

					BulletProofGenerators bg2 = new BulletProofGenerators(128, 1);

					long startVrf = System.currentTimeMillis();
					boolean match = verifyRangeProof(min, max, bitsize, BpProof, pc, bg2);
					long endVrf = System.currentTimeMillis();
					
					genTime[j] += endGen - startGen;
					vrfTime[j] += endVrf - startVrf;
					proofSize[j] += BpProof.size();

					System.out.println(match ? "Success" : "Fail");
				}
			}

			for(int j=0;j<Ns.length;j++) {
				System.out.println("N="+Ns[j]+", total generation time: "+(genTime[j] * 1. / 1000)+"s, average: "+(genTime[j] * 1. / 1000 / NN)+"s");
				System.out.println("N="+Ns[j]+", total verification time: "+(vrfTime[j] * 1. / 1000)+"s, average: "+(vrfTime[j] * 1. / 1000 / NN)+"s");
				System.out.println("N="+Ns[j]+", total proof size: "+(proofSize[j] * 1. / 1000)+"kB, average: "+(proofSize[j] * 1. / 1000 / NN)+"kB");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] a) {
		new BulletproofTests().run();
	}
}
