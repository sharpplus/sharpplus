package zk.sharp;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import decomp.SquareDecomp;
import ec.ECPoint;
import util.SharpUtils;
import zk.bulletproofs.PedersenCommitment;

public class SharpProver {
	
	Random generator;
	PedersenCommitment pc;
	ECPoint C_x;
	
	public ECPoint getCx() {
		return C_x;
	}
	
	public SharpProver() throws NoSuchAlgorithmException {
		pc = PedersenCommitment.getDefault();
		generator = new Random();
	}
	
	public SharpProof generateRangeProof(BigInteger[] x, BigInteger r_x, BigInteger B, int R, BigInteger L) {
		int N = x.length;
		
		BigInteger sum_x = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_x = sum_x.add(x[i]);
		}

		C_x = pc.commit(sum_x, r_x);
		
		/*** phase one ***/
		
		// line 1: determine the 3-square decomposition for all values
		
		SquareDecomp decomp = new SquareDecomp();
		decomp.loadPrecompDecomp2s();
		
		BigInteger[][] y = new BigInteger[N][3];

		for(int i=0;i<N;i++) {
			int[] decompVals = decomp.decomp3(x[i].intValueExact(), B.intValueExact());
			for(int j=0;j<3;j++) {
				y[i][j] = BigInteger.valueOf(decompVals[j]);
			}
			if(decompVals[0]*decompVals[0] + decompVals[1]*decompVals[1] + decompVals[2]*decompVals[2] != 4 * x[i].intValueExact() * (B.intValueExact() - x[i].intValueExact()) + 1) {
				System.out.println("Error: bad decomposition!");
			}
		}
		
		// line 2: draw random salt for C_y and masks
		
		BigInteger r_y = SharpUtils.randomBigInt(generator);
		BigInteger[] mu = new BigInteger[R];
		for(int k=0;k<R;k++) {
			mu[k] = SharpUtils.randomBigInt(generator).mod(L);
		}
		
		// line 3: compute C_y
		
		BigInteger sum_y_mu = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			for(int j=0;j<3;j++) {
				sum_y_mu = sum_y_mu.add(y[i][j]);
			}
		}
		for(int k=0;k<R;k++) {
			sum_y_mu = sum_y_mu.add(mu[k]);
		}

		ECPoint C_y = pc.commit(sum_y_mu, r_y);
		
		// line 1 (verifier): choose random challenge via Fiat-Shamir
		
		byte[] challenge1 = SharpUtils.fiatShamir(C_x, C_y, B, N, R);
		boolean[] gammas1 = SharpUtils.drawBooleansFromSeed(challenge1, N * 4 * R);
		
		boolean[][][] gamma = new boolean[N][4][R];
		
		for(int i=0;i<N;i++) {
			for(int j=0;j<4;j++) {
				for(int k=0;k<R;k++) {
					gamma[i][j][k] = gammas1[4 * R * i + R * j + k];
				}
			}
		}
		
		// line 4-5: compute zeta_k for all k
		
		BigInteger[] zeta = new BigInteger[R];
		for(int k=0;k<R;k++) {
			zeta[k] = BigInteger.ZERO;
			for(int i=0;i<N;i++) {
				for(int j=0;j<3;j++) {
					if(gamma[i][j][k]) {
						zeta[k] = zeta[k].add(y[i][j]);
					}
				}
				if(gamma[i][3][k]) {
					zeta[k] = zeta[k].add(x[i]);
				}
			}
			
			zeta[k] = SharpUtils.mask(zeta[k], mu[k]);
			
			// line 6:
			if(zeta[k].compareTo(BigInteger.ZERO) < 0) {
				// line 7: (not implemented)
				
			}
		}
		
		/*** phase two ***/
		
		// line 8: draw random values for \tilde{r}_x and \tilde{r}_y
		
		BigInteger tilde_rx = SharpUtils.randomBigInt(generator);
		BigInteger tilde_ry = SharpUtils.randomBigInt(generator);
				
		// line 9: draw random values \tilde{x} and \tilde{y} ---- Error: j \in [0,3] instead of j \in [1,3]
		
		BigInteger[] tilde_x = new BigInteger[N];
		BigInteger[][] tilde_y = new BigInteger[N][3];
		
		for(int i=0;i<N;i++) {
			tilde_x[i] = SharpUtils.randomBigInt(generator);
			for(int j=0;j<3;j++) {
				tilde_y[i][j] = SharpUtils.randomBigInt(generator);
			}
		}
		
		// line 10: draw random values \tilde{\mu}
		
		BigInteger[] tilde_mu = new BigInteger[R];
		
		for(int k=0;k<R;k++) {
			tilde_mu[k] = SharpUtils.randomBigInt(generator);
		}
		
		// line 11: compute d_k
		
		BigInteger[] d = new BigInteger[R];
		
		for(int k=0;k<R;k++) {
			d[k] = tilde_mu[k];
			for(int i=0;i<N;i++) {
				for(int j=0;j<3;j++) {
					if(gamma[i][j][k]) {
						d[k] = d[k].add(tilde_y[i][j]);
					}
				}
				if(gamma[i][3][k]) {
					d[k] = d[k].add(tilde_x[i]);
				}
			}
		}
		
		// line 12: compute D_x
		
		BigInteger sum_tx = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_tx = sum_tx.add(tilde_x[i]);
		}

		ECPoint D_x = pc.commit(sum_tx, tilde_rx);
		
		// line 13: compute D_y
		
		BigInteger sum_ty_tmu = BigInteger.ZERO;
		
		for(int i=0;i<N;i++) {
			for(int j=0;j<3;j++) {
				sum_ty_tmu = sum_ty_tmu.add(tilde_y[i][j]);
			}
		}
		for(int k=0;k<R;k++) {
			sum_ty_tmu = sum_ty_tmu.add(tilde_mu[k]);
		}
		
		ECPoint D_y = pc.commit(sum_ty_tmu, tilde_ry);
		
		// line 14: draw r^* and \tilde{r}^*
		
		BigInteger r_star = SharpUtils.randomBigInt(generator);
		BigInteger tilde_r_star = SharpUtils.randomBigInt(generator);
		
		// line 15: compute alpha^*_{1,i}
		
		BigInteger[] alpha_1_pos = new BigInteger[N];
		BigInteger[] alpha_1_neg = new BigInteger[N];
		for(int i=0;i<N;i++) {
			alpha_1_pos[i] = x[i].multiply(tilde_x[i]).multiply(BigInteger.valueOf(8));
			BigInteger sum_y_ty = BigInteger.ZERO;
			for(int j=0;j<3;j++) {
				sum_y_ty = sum_y_ty.add(y[i][j].multiply(tilde_y[i][j]));
			}
			alpha_1_pos[i] = alpha_1_pos[i].add(sum_y_ty.multiply(BigInteger.TWO));
			alpha_1_neg[i] = tilde_x[i].multiply(B).multiply(BigInteger.valueOf(4));
		}
		
		// line 16:  compute alpha^*_{0,i}
		
		BigInteger[] alpha_0 = new BigInteger[N];
		for(int i=0;i<N;i++) {
			alpha_0[i] = tilde_x[i].multiply(tilde_x[i]).multiply(BigInteger.valueOf(4));
			BigInteger sum_ty_ty = BigInteger.ZERO;
			for(int j=0;j<3;j++) {
				sum_ty_ty = sum_ty_ty.add(tilde_y[i][j].multiply(tilde_y[i][j]));
			}
			alpha_0[i] = alpha_0[i].add(sum_ty_ty);
		}
		
		// line 17: compute C^*
		
		BigInteger sum_alpha_1_pos = BigInteger.ZERO;
		BigInteger sum_alpha_1_neg = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_alpha_1_pos = sum_alpha_1_pos.add(alpha_1_pos[i]);
			sum_alpha_1_neg = sum_alpha_1_neg.add(alpha_1_neg[i]);
		}
		
		ECPoint ec_sum_alpha_1_pos = pc.commit(sum_alpha_1_pos, r_star.multiply(BigInteger.TWO)).decompress();
		ECPoint ec_sum_alpha_1_neg = pc.commit(sum_alpha_1_neg, r_star).decompress();
		ECPoint C_star = ec_sum_alpha_1_pos.subtract(ec_sum_alpha_1_neg).compress();
		
		// line 18: compute D^*
		
		BigInteger sum_alpha_0 = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_alpha_0 = sum_alpha_0.add(alpha_0[i]);
		}
		ECPoint D_star = pc.commit(sum_alpha_0, tilde_r_star);
		
		// line 4 (verifier): choose random challenge via Fiat-Shamir (rename to gamma^* because gamma already exists)
		
		byte[] challenge2 = SharpUtils.fiatShamir(C_x, C_y, B, N, R);
		BigInteger gamma_star = SharpUtils.drawNumberFromSeed(challenge2);
		
//		BigInteger gamma_star = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));

		// line 19, 20: generate z_i and z_{i,j} 
		BigInteger[] z_x = new BigInteger[N];
		BigInteger[][] z_y = new BigInteger[N][3];	
		
		for(int i=0;i<N;i++) {
			z_x[i] = SharpUtils.mask(gamma_star.multiply(x[i]), tilde_x[i]);
			for(int j=0;j<3;j++) {
				z_y[i][j] = SharpUtils.mask(gamma_star.multiply(y[i][j]), tilde_y[i][j]);
			}
		}
		
		// line 21: generate t_x and t_y
	
		BigInteger t_x = SharpUtils.mask(gamma_star.multiply(r_x), tilde_rx);
		BigInteger t_y = SharpUtils.mask(gamma_star.multiply(r_y), tilde_ry);
		
		// line 22: generate t_star
		
		BigInteger t_star = SharpUtils.mask(gamma_star.multiply(r_star), tilde_r_star);
		
		// line 23: generate tau_k
		
		BigInteger[] tau = new BigInteger[R];
		for(int k=0;k<R;k++) {
			tau[k] = SharpUtils.mask(gamma_star.multiply(mu[k]), tilde_mu[k]);
		}
		
		// line 24 and 25 (not implemented)
		
//		System.out.println(gamma_star);
		
		return new SharpProof(C_y, D_x, D_y, C_star, D_star, t_x, t_y, t_star, zeta, z_x, z_y, tau, d);
	}
}
