package zk.sharpplus;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import decomp.SquareDecomp;
import ec.ECPoint;
import util.SharpUtils;
import zk.bulletproofs.PedersenCommitment;

public class SharpPlusProver {
	
	Random generator;
	PedersenCommitment pc;
	ECPoint[] C_x;
	ECPoint[] C_x_prime_1;
	ECPoint[] C_x_prime_2;
	
	public SharpPlusProver() throws NoSuchAlgorithmException {
		pc = PedersenCommitment.getDefault();
		generator = new Random();
	}
	
	public ECPoint[] getCx() {
		return C_x;
	}
	
	public ECPoint[] getCxprime1() {
		return C_x_prime_1;
	}
	
	public ECPoint[] getCxprime2() {
		return C_x_prime_2;
	}
	
	public SharpPlusProof generateComboProof(BigInteger[] x, BigInteger[] x_prime_1, BigInteger[] x_prime_2, BigInteger[] r_x, BigInteger[] r_x_prime_1, BigInteger[] r_x_prime_2, int R, BigInteger L) {
		int N = x.length;
		int M = x_prime_1.length;
		
		C_x = new ECPoint[N];
		C_x_prime_1 = new ECPoint[M];
		C_x_prime_2 = new ECPoint[M];
		
		for(int i=0;i<N;i++) {
			C_x[i] = pc.commit(x[i], r_x[i]);
		}
		
		for(int i=0;i<M;i++) {
			C_x_prime_1[i] = pc.commit(x_prime_1[i], r_x_prime_1[i]);
			C_x_prime_2[i] = pc.commit(x_prime_2[i], r_x_prime_2[i]);
		}
		
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
		
		// line 2: generate the multiplications
		
		BigInteger[] y_prime = new BigInteger[M];
		for(int i=0;i<M;i++) {
			y_prime[i] = x_prime_1[i].multiply(x_prime_2[i]);
		}
		
		// line 3: draw random salt for C_y and masks
		
		BigInteger r_y = SharpUtils.randomBigInt(generator);
		BigInteger r_y_prime = SharpUtils.randomBigInt(generator);
		
		BigInteger[] mu = new BigInteger[R];
		for(int k=0;k<R;k++) {
			mu[k] = SharpUtils.randomBigInt(generator).mod(L);
		}
		
		// line 4: compute C_y
		
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
		
		// line 5: compute C'_y
		
		BigInteger sum_y_prime = BigInteger.ZERO;
		for(int i=0;i<M;i++) {
			sum_y_prime = sum_y_prime.add(y_prime[i]);
		}
		
		ECPoint C_y_prime = pc.commit(sum_y_prime, r_y_prime);
		
		// line 1 (verifier): choose random challenge via Fiat-Shamir
		
		byte[] challenge1 = SharpUtils.fiatShamir(C_x, C_y, C_y_prime, N, R);
		boolean[] gammas1 = SharpUtils.drawBooleansFromSeed(challenge1, N * 4 * R);
		
		boolean[][][] gamma = new boolean[N][3][R];
		
		for(int i=0;i<N;i++) {
			for(int j=0;j<3;j++) {
				for(int k=0;k<R;k++) {
					gamma[i][j][k] = gammas1[3 * R * i + R * j + k];
				}
			}
		}
		
		// line 6: compute zeta_k for all k
		
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
		}
		
		/*** phase two ***/
		
		// line 7: draw random values for \tilde{r}_x and \tilde{r}_y
		
		
		BigInteger[] tilde_rx = new BigInteger[N];
		BigInteger[] tilde_rx_prime_1 = new BigInteger[M];;
		BigInteger[] tilde_rx_prime_2 = new BigInteger[M];
		BigInteger tilde_ry = SharpUtils.randomBigInt(generator);
		
		for(int i=0;i<N;i++) {
			tilde_rx[i] = SharpUtils.randomBigInt(generator);
		}
		for(int i=0;i<M;i++) {
			tilde_rx_prime_1[i] = SharpUtils.randomBigInt(generator);
			tilde_rx_prime_2[i] = SharpUtils.randomBigInt(generator);
		}
				
		// line 8-11: draw random values \tilde{x}, \tilde{x}', \tilde{y} and \tilde{y}'
		
		BigInteger[] tilde_x = new BigInteger[N];
		BigInteger[][] tilde_y = new BigInteger[N][3];
		
		for(int i=0;i<N;i++) {
			tilde_x[i] = SharpUtils.randomBigInt(generator);
			for(int j=0;j<3;j++) {
				tilde_y[i][j] = SharpUtils.randomBigInt(generator);
			}
		}
		
		BigInteger[] tilde_y_prime = new BigInteger[N];
		BigInteger[] tilde_x_prime_1 = new BigInteger[N];
		BigInteger[] tilde_x_prime_2 = new BigInteger[N];
		
		for(int i=0;i<N;i++) {
			tilde_y_prime[i] = SharpUtils.randomBigInt(generator);
			tilde_x_prime_1[i] = SharpUtils.randomBigInt(generator);
			tilde_x_prime_2[i] = SharpUtils.randomBigInt(generator);
		}
		
		// line 12: draw random values \tilde{\mu}
		
		BigInteger[] tilde_mu = new BigInteger[R];
		
		for(int k=0;k<R;k++) {
			tilde_mu[k] = SharpUtils.randomBigInt(generator);
		}
		
		// line 13: compute d_k
		
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
		
		// line 14: compute D_x

		ECPoint[] D_x = new ECPoint[N];
		ECPoint[] D_x_prime_1 = new ECPoint[M];
		ECPoint[] D_x_prime_2 = new ECPoint[M];
		
		for(int i=0;i<N;i++) {
			D_x[i] = pc.commit(tilde_x[i], tilde_rx[i]);
		}
		for(int i=0;i<M;i++) {
			D_x_prime_1[i] = pc.commit(tilde_x_prime_1[i], tilde_rx_prime_1[i]);
			D_x_prime_2[i] = pc.commit(tilde_x_prime_2[i], tilde_rx_prime_2[i]);
		}
		
		// line 15: compute D_y
		
		BigInteger sum_ty_tmu = BigInteger.ZERO;
		
		for(int i=0;i<N;i++) {
			for(int j=0;j<3;j++) {
				sum_ty_tmu = sum_ty_tmu.add(tilde_y[i][j]);
			}
		}
		for(int i=0;i<M;i++) {
			sum_ty_tmu = sum_ty_tmu.add(tilde_y_prime[i]);
		}
		for(int k=0;k<R;k++) {
			sum_ty_tmu = sum_ty_tmu.add(tilde_mu[k]);
		}
		
		ECPoint D_y = pc.commit(sum_ty_tmu, tilde_ry);
		
		// line 14: draw r^* and \tilde{r}^*
		
		BigInteger[] r_star = new BigInteger[N];
		BigInteger[] tilde_r_star = new BigInteger[N];
		BigInteger[] r_star_prime = new BigInteger[M];
		BigInteger[] tilde_r_star_prime = new BigInteger[M];
		
		for(int i=0;i<N;i++) {
			r_star[i] = SharpUtils.randomBigInt(generator);
			tilde_r_star[i] = SharpUtils.randomBigInt(generator);
		}
		for(int i=0;i<M;i++) {
			r_star_prime[i] = SharpUtils.randomBigInt(generator);
			tilde_r_star_prime[i] = SharpUtils.randomBigInt(generator);
		}
		
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
		
		BigInteger[] beta_0 = new BigInteger[N];
		BigInteger[] beta_1 = new BigInteger[N];
		
		for(int i=0;i<M;i++) {
			beta_1[i] = (x_prime_1[i].multiply(tilde_x_prime_2[i]).add(x_prime_2[i].multiply(tilde_x_prime_1[i]))).multiply(BigInteger.valueOf(-1));
			beta_0[i] = tilde_y_prime[i].subtract(tilde_x_prime_1[i].multiply(tilde_x_prime_2[i]));
		}
		
		// line 17: compute C^*
		
		ECPoint C_star[] = new ECPoint[N];
		ECPoint C_star_prime[] = new ECPoint[M];
		
		for(int i=0;i<N;i++) {
			C_star[i] = pc.commit(alpha_1[i], r_star[i]);
		}
		for(int i=0;i<M;i++) {
			C_star_prime[i] = pc.commit(beta_1[i], r_star_prime[i]);
		}
		
		// line 18: compute D^*
		
		ECPoint D_star[] = new ECPoint[N];
		ECPoint D_star_prime[] = new ECPoint[M];
		
		for(int i=0;i<N;i++) {
			D_star[i] = pc.commit(alpha_0[i], tilde_r_star[i]);
		}
		for(int i=0;i<M;i++) {
			D_star_prime[i] = pc.commit(beta_0[i], tilde_r_star_prime[i]);
		}
		
		// line 4 (verifier): choose random challenge via Fiat-Shamir (rename to gamma^* because gamma already exists)
		
		byte[] challenge2 = SharpUtils.fiatShamir(C_x, C_y, C_y_prime, N, R);
		BigInteger gamma_star = SharpUtils.drawNumberFromSeed(challenge2);
		BigInteger gamma_square = gamma_star.modPow(BigInteger.TWO, PedersenCommitment.GROUP_ORDER);

		// line 19, 20: generate z_i and z_{i,j} 
		BigInteger[] z_x = new BigInteger[N];
		BigInteger[][] z_y = new BigInteger[N][3];	
		
		for(int i=0;i<N;i++) {
			z_x[i] = SharpUtils.mask(gamma_square.multiply(x[i]), tilde_x[i]);
			for(int j=0;j<3;j++) {
				z_y[i][j] = SharpUtils.mask(gamma_star.multiply(y[i][j]), tilde_y[i][j]);
			}
		}
		
		BigInteger[] z_y_prime = new BigInteger[M];
		BigInteger[] z_x_prime_1 = new BigInteger[M];
		BigInteger[] z_x_prime_2 = new BigInteger[M];
		
		for(int i=0;i<M;i++) {
			z_y_prime[i] = SharpUtils.mask(gamma_square.multiply(y_prime[i]), tilde_y_prime[i]);
			z_x_prime_1[i] = SharpUtils.mask(gamma_star.multiply(x_prime_1[i]), tilde_x_prime_1[i]);
			z_x_prime_2[i] = SharpUtils.mask(gamma_star.multiply(x_prime_2[i]), tilde_x_prime_2[i]);
		}
		
		// line 21: generate t_x and t_y
	
		BigInteger sum_rx = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_rx = sum_rx.add(r_x[i].multiply(gamma_star.modPow(BigInteger.valueOf(i),PedersenCommitment.GROUP_ORDER)));
		}
		BigInteger sum_rx_prime = BigInteger.ZERO;
		for(int i=0;i<M;i++) {
			sum_rx_prime = sum_rx_prime.add(r_x_prime_1[i].multiply(gamma_star.modPow(BigInteger.valueOf(N+i),PedersenCommitment.GROUP_ORDER)));
			sum_rx_prime = sum_rx_prime.add(r_x_prime_2[i].multiply(gamma_star.modPow(BigInteger.valueOf(N+M+i),PedersenCommitment.GROUP_ORDER)));
		}
		BigInteger sum_tilde_rx = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_tilde_rx = sum_tilde_rx.add(tilde_rx[i].multiply(gamma_star.modPow(BigInteger.valueOf(i),PedersenCommitment.GROUP_ORDER)));
		}
		for(int i=0;i<M;i++) {
			sum_tilde_rx = sum_tilde_rx.add(tilde_rx_prime_1[i].multiply(gamma_star.modPow(BigInteger.valueOf(N+i),PedersenCommitment.GROUP_ORDER)));
			sum_tilde_rx = sum_tilde_rx.add(tilde_rx_prime_2[i].multiply(gamma_star.modPow(BigInteger.valueOf(N+M+i),PedersenCommitment.GROUP_ORDER)));
		}
		
		BigInteger t_x = SharpUtils.mask(gamma_square.multiply(sum_rx).add(gamma_star.multiply(sum_rx_prime)), sum_tilde_rx);
		BigInteger t_y = SharpUtils.mask(gamma_star.multiply(r_y).add(r_y_prime.multiply(gamma_square)), tilde_ry);
		
		// line 22: generate t_star
		
		BigInteger sum_r_star = BigInteger.ZERO;
		BigInteger sum_tilde_r_star = BigInteger.ZERO;
		BigInteger sum_r_star_prime = BigInteger.ZERO;
		BigInteger sum_tilde_r_star_prime = BigInteger.ZERO;
		
		for(int i=0;i<N;i++) {
			sum_r_star = sum_r_star.add(r_star[i].multiply(gamma_star.modPow(BigInteger.valueOf(i),PedersenCommitment.GROUP_ORDER)));
			sum_tilde_r_star = sum_tilde_r_star.add(tilde_r_star[i].multiply(gamma_star.modPow(BigInteger.valueOf(i),PedersenCommitment.GROUP_ORDER)));
		}
		for(int i=0;i<M;i++) {
			sum_r_star_prime = sum_r_star_prime.add(r_star_prime[i].multiply(gamma_star.modPow(BigInteger.valueOf(N+i),PedersenCommitment.GROUP_ORDER)));
			sum_tilde_r_star_prime = sum_tilde_r_star_prime.add(tilde_r_star_prime[i].multiply(gamma_star.modPow(BigInteger.valueOf(N+i),PedersenCommitment.GROUP_ORDER)));
		}
		
		BigInteger t_star = SharpUtils.mask(gamma_star.multiply(sum_r_star.add(sum_r_star_prime)), sum_tilde_r_star.add(sum_tilde_r_star_prime));
		
		// line 23: generate tau_k
		
		BigInteger[] tau = new BigInteger[R];
		for(int k=0;k<R;k++) {
			tau[k] = SharpUtils.mask(gamma_star.multiply(mu[k]), tilde_mu[k]);
		}
		
		return new SharpPlusProof(C_y, C_y_prime, D_x, D_x_prime_1, D_x_prime_2, D_y, C_star, C_star_prime, D_star, D_star_prime, t_x, t_y, t_star, zeta, z_x, z_y, z_y_prime, z_x_prime_1, z_x_prime_2, tau, d);
	}
}
