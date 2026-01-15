package experiments;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import ec.ECPoint;
import zk.bulletproofs.PedersenCommitment;

public class CryptoPerformanceTests {

	public void testCommitGenSpeed(PedersenCommitment pc, int N) {		
		Random generator = new Random(100001);
		long totTime = 0;

		for(int i=0;i<N;i++) {
			BigInteger x1 = BigInteger.valueOf(generator.nextLong());
			BigInteger r1 = BigInteger.valueOf(generator.nextLong());

			long startVrf = System.currentTimeMillis(); 
			pc.commit(x1, r1).decompress();
			long endVrf = System.currentTimeMillis();

			totTime += endVrf - startVrf;
		}

		System.out.println("Time per commit gen: "+(totTime*1./1000/N)+" s");
	}
	
	public void testCommitSumSpeed(PedersenCommitment pc, int N) {		
		Random generator = new Random(100001);
		long totTime = 0;

		for(int i=0;i<N;i++) {
			BigInteger x1 = BigInteger.valueOf(generator.nextLong());
			BigInteger r1 = BigInteger.valueOf(generator.nextLong());
			BigInteger x2 = BigInteger.valueOf(generator.nextLong());
			BigInteger r2 = BigInteger.valueOf(generator.nextLong());
			
			ECPoint e1 = pc.commit(x1, r1).decompress();
			ECPoint e2 = pc.commit(x2, r2).decompress();

			long startVrf = System.currentTimeMillis(); 
			e1.add(e2);
			long endVrf = System.currentTimeMillis();

			totTime += endVrf - startVrf;
		}

		System.out.println("Time per commit sum: "+(totTime*1./1000/N)+" s");
	}

	public static void main(String[] a) {
		PedersenCommitment pc = null;
		try {
			pc = PedersenCommitment.getDefault();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		new CryptoPerformanceTests().testCommitGenSpeed(pc, 10000);
		new CryptoPerformanceTests().testCommitSumSpeed(pc, 10000);
	}
}
