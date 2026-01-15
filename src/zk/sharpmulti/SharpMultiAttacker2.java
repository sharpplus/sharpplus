package zk.sharpmulti;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import ec.ECPoint;
import util.SharpUtils;
import zk.bulletproofs.PedersenCommitment;
import zk.bulletproofs.BpProver;
import zk.bulletproofs.Transcript;

public class SharpMultiAttacker2 {
	
	Random generator;
	
	PedersenCommitment pc;
	Transcript transcript;
	BpProver prover;
	
	public SharpMultiAttacker2() throws NoSuchAlgorithmException {
		pc = PedersenCommitment.getDefault();
		generator = new Random();
		transcript = new Transcript();
	}
	
	public SharpMultiProof generateMultiProof(BigInteger[] x_base, BigInteger[] y1, BigInteger[] y2, BigInteger r_x) {
		int N = x_base.length;
		
		BigInteger[] x = new BigInteger[N];
		for(int i=0;i<N;i++) {
			x[i] = x_base[i];
		}
		
		BigInteger sum_x = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_x = sum_x.add(x[i]);
		}

		ECPoint C_x = pc.commit(sum_x, r_x);
		
		BigInteger r_y = SharpUtils.randomBigInt(generator);
		
		BigInteger sum_y_mu = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_y_mu = sum_y_mu.add(y1[i]);
			sum_y_mu = sum_y_mu.add(y2[i]);
		}

		ECPoint C_y = pc.commit(sum_y_mu, r_y);
		
		/*** phase one ***/
		
//		byte[] challenge2 = SharpUtils.fiatShamir(C_x, C_y, N);
//		BigInteger gamma_star = SharpUtils.drawNumberFromSeed(new byte[] {});
		
		byte[] challenge2 = SharpUtils.fiatShamir(C_x, C_y, N);
		BigInteger gamma_star = SharpUtils.drawNumberFromSeed(challenge2);
		
		BigInteger[] tilde_x = new BigInteger[N];
		BigInteger[] tilde_y1 = new BigInteger[N];
		BigInteger[] tilde_y2 = new BigInteger[N];
		
		for(int i=0;i<N;i++) {
			tilde_x[i] = SharpUtils.randomBigInt(generator);
			tilde_y1[i] = SharpUtils.randomBigInt(generator);
			tilde_y2[i] = SharpUtils.randomBigInt(generator);
		}
		
		BigInteger[] z_x = new BigInteger[N];
		BigInteger[] z_y1 = new BigInteger[N];
		BigInteger[] z_y2 = new BigInteger[N];
		for(int i=0;i<N;i++) {
			z_x[i] = SharpUtils.mask(gamma_star.multiply(gamma_star.multiply(x[i])), tilde_x[i]);
			z_y1[i] = SharpUtils.mask(gamma_star.multiply(y1[i]), tilde_y1[i]);
			z_y2[i] = SharpUtils.mask(gamma_star.multiply(y2[i]), tilde_y2[i]);
		}
		
		BigInteger r_star = SharpUtils.randomBigInt(generator);
		BigInteger tilde_r_star = SharpUtils.randomBigInt(generator);
		BigInteger t_star = SharpUtils.mask(gamma_star.multiply(r_star), tilde_r_star);
		
		BigInteger[] alpha_0 = new BigInteger[N];
		BigInteger[] alpha_1 = new BigInteger[N];
		
		for(int i=0;i<N;i++) {
			alpha_1[i] = (y1[i].multiply(tilde_y2[i]).add(y2[i].multiply(tilde_y1[i]))).multiply(BigInteger.valueOf(-1));
			alpha_0[i] = tilde_x[i].subtract(tilde_y1[i].multiply(tilde_y2[i]));
		}
		
		// compute D_x
		
		BigInteger tilde_rx = SharpUtils.randomBigInt(generator);
		BigInteger tilde_ry = SharpUtils.randomBigInt(generator);
		
		BigInteger sum_tx = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_tx = sum_tx.add(tilde_x[i]);
		}

		ECPoint D_x = pc.commit(sum_tx, tilde_rx);
		
		// line 13: compute D_y
		
		BigInteger sum_ty_tmu = BigInteger.ZERO;
		
		for(int i=0;i<N;i++) {
			sum_ty_tmu = sum_ty_tmu.add(tilde_y1[i]);
			sum_ty_tmu = sum_ty_tmu.add(tilde_y2[i]);
		}
		
		ECPoint D_y = pc.commit(sum_ty_tmu, tilde_ry);
		
		// compute C^*
		
		BigInteger sum_alpha_1 = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_alpha_1 = sum_alpha_1.add(alpha_1[i]);
		}
		ECPoint C_star = pc.commit(sum_alpha_1, r_star);
		
		// compute D^*
		
		BigInteger sum_alpha_0 = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_alpha_0 = sum_alpha_0.add(alpha_0[i]);
		}
		ECPoint D_star = pc.commit(sum_alpha_0, tilde_r_star);
		
		BigInteger t_x = SharpUtils.mask(gamma_star.multiply(gamma_star.multiply(r_x)), tilde_rx);
		BigInteger t_y = SharpUtils.mask(gamma_star.multiply(r_y), tilde_ry);
		
		
		return new SharpMultiProof(C_x, C_y, D_x, D_y, C_star, D_star, t_x, t_y, t_star, z_x, z_y1, z_y2);
	}
}
