package zk.sharpmultibackup;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import ec.ECPoint;
import util.SharpUtils;
import zk.bulletproofs.PedersenCommitment;
import zk.bulletproofs.Utils;

public class SharpMultiVerifier {
	
	Random generator;
	PedersenCommitment pc;
	
	public SharpMultiVerifier() throws NoSuchAlgorithmException {
		pc = PedersenCommitment.getDefault();
		generator = new Random();
	}
	
	public boolean verify(SharpMultiProof p) {
		int N = p.z_x.length;

		byte[] challenge2 = SharpUtils.fiatShamir(p.C_x, p.C_y, N);
		BigInteger gamma_star = SharpUtils.drawNumberFromSeed(challenge2);
		
		// check C_x
		
		BigInteger sum_z_x = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_z_x = sum_z_x.add(p.z_x[i]);
		}
		
		
		ECPoint F_x_term1 = pc.commit(sum_z_x, p.t_x).decompress();
		ECPoint F_x_term2 = p.C_x.decompress().multiply(Utils.scalar(gamma_star.multiply(gamma_star)));
		ECPoint F_x = F_x_term1.subtract(F_x_term2);
		
//		System.out.println(F_x.equals(p.D_x.decompress()));
		if (!F_x.equals(p.D_x.decompress())) return false;
		
		// check C_y
		
		BigInteger sum_z_y_tau = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_z_y_tau = sum_z_y_tau.add(p.z_y1[i].add(p.z_y2[i]));
		}
		
		ECPoint F_y_term1 = pc.commit(sum_z_y_tau, p.t_y).decompress();
		ECPoint F_y_term2 = p.C_y.decompress().multiply(Utils.scalar(gamma_star));
		ECPoint F_y = F_y_term1.subtract(F_y_term2);
		
//		System.out.println(F_y.equals(p.D_y.decompress()));
		if(!F_y.equals(p.D_y.decompress())) return false;

		// check C_x
		
		BigInteger[] f_star = new BigInteger[N];
		for(int i=0;i<N;i++) {
			f_star[i] = p.z_x[i].subtract(p.z_y1[i].multiply(p.z_y2[i]));
		}
		
		BigInteger sum_fstar = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_fstar = sum_fstar.add(f_star[i]);
		}
		
		ECPoint F_star_term1 = pc.commit(sum_fstar, p.t_star).decompress();
		ECPoint F_star_term2 = p.C_star.decompress().multiply(Utils.scalar(gamma_star));
		ECPoint F_star = F_star_term1.subtract(F_star_term2);
		
//		System.out.println(F_star.equals(p.D_star.decompress()));
		if(!F_star.equals(p.D_star.decompress())) return false;
		
		return true;
	}

}
