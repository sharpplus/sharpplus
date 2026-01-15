package zk.sharp1s;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import decomp.SquareDecomp;
import ec.ECPoint;
import util.SharpUtils;
import zk.bulletproofs.PedersenCommitment;

public class Sharp1sProver {
	
	Random generator;
	
	PedersenCommitment pc;
	
	public Sharp1sProver() throws NoSuchAlgorithmException {
		pc = PedersenCommitment.getDefault();
		generator = new Random();
	}
	
	public Sharp1sProof generateRangeProof(BigInteger[] x, BigInteger r_x, int R, BigInteger L) {
		int N = x.length;
		
		BigInteger sum_x = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_x = sum_x.add(x[i]);
		}

		ECPoint C_x = pc.commit(sum_x, r_x);
		
		/*** phase one ***/
		
		// line 1: determine the 3-square decomposition for all values
		
		SquareDecomp decomp = new SquareDecomp();
		decomp.loadPrecompDecomp2s();
		
		BigInteger[][] y = new BigInteger[N][3];

		for(int i=0;i<N;i++) {
			int[] decompVals = decomp.decomp3(x[i].intValueExact());
			for(int j=0;j<3;j++) {
				y[i][j] = BigInteger.valueOf(decompVals[j]);
			}
			if(decompVals[0]*decompVals[0] + decompVals[1]*decompVals[1] + decompVals[2]*decompVals[2] != 4 * x[i].intValueExact() + 1) {
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
		
		byte[] challenge1 = SharpUtils.fiatShamir(C_x, C_y, N, R);
		boolean[] gammas1 = SharpUtils.drawBooleansFromSeed(challenge1, N * 4 * R);
		
		boolean[][][] gamma = new boolean[N][3][R];
		
		for(int i=0;i<N;i++) {
			for(int j=0;j<3;j++) {
				for(int k=0;k<R;k++) {
					gamma[i][j][k] = gammas1[3 * R * i + R * j + k];
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
		
		BigInteger[] alpha_1 = new BigInteger[N];
		for(int i=0;i<N;i++) {
			alpha_1[i] = BigInteger.ZERO;
			for(int j=0;j<3;j++) {
				alpha_1[i] = alpha_1[i].subtract(y[i][j].multiply(tilde_y[i][j]));
			}
			alpha_1[i] = alpha_1[i].multiply(BigInteger.TWO);
		}
		
		// line 16:  compute alpha^*_{0,i}
		
		BigInteger[] alpha_0 = new BigInteger[N];
		for(int i=0;i<N;i++) {
			alpha_0[i] = tilde_x[i].multiply(BigInteger.valueOf(4));
			BigInteger sum_ty_ty = BigInteger.ZERO;
			for(int j=0;j<3;j++) {
				sum_ty_ty = sum_ty_ty.add(tilde_y[i][j].multiply(tilde_y[i][j]));
			}
			alpha_0[i] = alpha_0[i].subtract(sum_ty_ty);
		}
		
		// line 17: compute C^*
		
		BigInteger sum_alpha_1 = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_alpha_1 = sum_alpha_1.add(alpha_1[i]);
		}
		ECPoint C_star = pc.commit(sum_alpha_1, r_star);
		
		// line 18: compute D^*
		
		BigInteger sum_alpha_0 = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_alpha_0 = sum_alpha_0.add(alpha_0[i]);
		}
		ECPoint D_star = pc.commit(sum_alpha_0, tilde_r_star);
		
		// line 4 (verifier): choose random challenge via Fiat-Shamir (rename to gamma^* because gamma already exists)
		
		byte[] challenge2 = SharpUtils.fiatShamir(C_x, C_y, N, R);
		BigInteger gamma_star = SharpUtils.drawNumberFromSeed(challenge2);
		
		
//		BigInteger gamma_star = BigInteger.valueOf(generator.nextLong(Long.MAX_VALUE));

		// line 19, 20: generate z_i and z_{i,j} 
		BigInteger[] z_x = new BigInteger[N];
		BigInteger[][] z_y = new BigInteger[N][3];	
		
		for(int i=0;i<N;i++) {
			z_x[i] = SharpUtils.mask(gamma_star.multiply(gamma_star.multiply(x[i])), tilde_x[i]);
			for(int j=0;j<3;j++) {
				z_y[i][j] = SharpUtils.mask(gamma_star.multiply(y[i][j]), tilde_y[i][j]);
			}
		}
		
		// line 21: generate t_x and t_y
	
		BigInteger t_x = SharpUtils.mask(gamma_star.multiply(gamma_star.multiply(r_x)), tilde_rx);
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
		
		// Test
		
		BigInteger[] f_star = new BigInteger[N];
		for(int i=0;i<N;i++) {
			f_star[i] = BigInteger.valueOf(4).multiply(z_x[i]).add(gamma_star.multiply(gamma_star)).subtract(z_y[i][0].multiply(z_y[i][0]).add(z_y[i][1].multiply(z_y[i][1])).add(z_y[i][2].multiply(z_y[i][2])));
		}
		
		// line 10: compute F_star
		BigInteger sum_fstar = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_fstar = sum_fstar.add(f_star[i]);
		}

//		System.out.println(" "+t_star.subtract(r_star.multiply(gamma_star)).subtract(tilde_r_star));
		
		for(int i=0;i<N;i++) {
//			System.out.println(i+" "+z_x[i].equals(x[i].multiply(gamma_star).multiply(gamma_star).add(tilde_x[i])));
//			System.out.println(i+" "+alpha_0[i].equals(BigInteger.valueOf(4).multiply(tilde_x[i]).subtract(tilde_y[i][0].multiply(tilde_y[i][0]).add(tilde_y[i][1].multiply(tilde_y[i][1])).add(tilde_y[i][2].multiply(tilde_y[i][2])))));
//			System.out.println(i+" "+alpha_1[i].equals(BigInteger.valueOf(-2).multiply(y[i][0].multiply(tilde_y[i][0]).add(y[i][1].multiply(tilde_y[i][1])).add(y[i][2].multiply(tilde_y[i][2])))));
//			System.out.println(i+" "+f_star[i].subtract(alpha_0[i]).subtract(alpha_1[i].multiply(gamma_star)));
			for(int j=0;j<3;j++) {
//			System.out.println(i+" "+j+" "+z_y[i][j].equals(y[i][j].multiply(gamma_star).add(tilde_y[i][j])));
			}
		}
		
		return new Sharp1sProof(C_x, C_y, D_x, D_y, C_star, D_star, t_x, t_y, t_star, zeta, z_x, z_y, tau, d);
	}
}
