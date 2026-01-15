package zk.sharp;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import decomp.SquareDecomp;
import ec.ECPoint;
import util.SharpUtils;
import zk.bulletproofs.PedersenCommitment;
import zk.bulletproofs.Utils;

public class SharpAttacker2 {

	Random generator;
	PedersenCommitment pc;
	ECPoint C_x;
	
	public ECPoint getCx() {
		return C_x;
	}

	public SharpAttacker2() throws NoSuchAlgorithmException {
		pc = PedersenCommitment.getDefault();
		generator = new Random();
	}

	public BigInteger[] getDistort(BigInteger[][] y, int slack, int idx) {
		BigInteger[] distort = new BigInteger[4];
		for(int i=0;i<4;i++) distort[i] = BigInteger.valueOf(-1);

		int mmax = 50;

		for(int i=0;i<mmax;i++) {
			BigInteger yi = y[1][idx].subtract(BigInteger.valueOf(i));
			BigInteger idif = yi.multiply(yi).subtract(y[1][idx].multiply(y[1][idx]));
			for(int j=0;j<mmax;j++) {
				BigInteger yj = y[2][idx].subtract(BigInteger.valueOf(j));
				BigInteger jdif = yj.multiply(yj).subtract(y[2][idx].multiply(y[2][idx]));
				for(int k=0;k<mmax;k++) {
					BigInteger yk = y[3][idx].subtract(BigInteger.valueOf(k));
					BigInteger kdif = yk.multiply(yk).subtract(y[3][idx].multiply(y[3][idx]));
					for(int l=0;l<mmax;l++) {
						BigInteger yl = y[4][idx].subtract(BigInteger.valueOf(l));
						BigInteger ldif = yl.multiply(yl).subtract(y[4][idx].multiply(y[4][idx]));
						if(yi.compareTo(BigInteger.ZERO) >= 0 && yj.compareTo(BigInteger.ZERO) >= 0 && yk.compareTo(BigInteger.ZERO) >= 0 && yl.compareTo(BigInteger.ZERO) >= 0) {
							if(idif.add(jdif).add(kdif).add(ldif).equals(BigInteger.valueOf(slack))) {
								System.out.println("Success: "+i+" "+j+" "+k+" "+l);

								if(distort[0].equals(BigInteger.valueOf(-1))) {
									distort[0] = yi;
									distort[1] = yj;
									distort[2] = yk;
									distort[3] = yl;
								}
							}
						}
					}
				}
			}
		}

		System.out.println(Arrays.toString(distort));
		return distort;
	}

	public SharpProof generateRangeProof(BigInteger[] x_base, BigInteger r_x, BigInteger B, int R, BigInteger L) {
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

		/*** phase one ***/

		// line 1: determine the 3-square decomposition for all values

		SquareDecomp decomp = new SquareDecomp();
		decomp.loadPrecompDecomp2s();

		BigInteger[][] y = new BigInteger[N][3];

		int slack = 0;

		for(int i=0;i<N;i++) {
			int[] decompVals = decomp.decomp3(x[i].intValueExact(), B.intValueExact());
			for(int j=0;j<3;j++) {
				y[i][j] = BigInteger.valueOf(decompVals[j]);
			}
			if(decompVals[0]*decompVals[0] + decompVals[1]*decompVals[1] + decompVals[2]*decompVals[2] != 4 * x[i].intValueExact() * (B.intValueExact() - x[i].intValueExact()) + 1) {
				System.out.println("Error: bad decomposition!");
			}
			System.out.println(Arrays.toString(y[i])+" "+((1 + 4 * x[i].intValueExact() * (B.intValueExact() - x[i].intValueExact()) - (decompVals[0]*decompVals[0] + decompVals[1]*decompVals[1] + decompVals[2]*decompVals[2]))));
		}

		y[0][0] = BigInteger.valueOf(0);
		y[0][1] = BigInteger.valueOf(0);
		y[0][2] = BigInteger.valueOf(0);
		slack = (4 * x[0].intValueExact() * (B.intValueExact() - x[0].intValueExact()) + 1) - (y[0][0].multiply(y[0][0]).intValueExact() + y[0][1].multiply(y[0][1]).intValueExact() + y[0][2].multiply(y[0][2]).intValueExact());
		System.out.println("slack: 4 * "+x[0].intValueExact()+" * ("+B.intValueExact()+" - "+x[0].intValueExact()+")");
		System.out.println("slack: "+slack);
		
		int idx = 0;

		BigInteger[] distort = getDistort(y, slack, idx);

		if(!distort[0].equals(BigInteger.valueOf(-1))) {
			for(int i=0;i<distort.length;i++) {
				y[i+1][idx] = distort[i];
			}
		}

		//		BigInteger sum = BigInteger.ZERO;
//		for(int i=0;i<N;i++) {
//			BigInteger xx = x[i].multiply(B.subtract(x[i])).multiply(BigInteger.valueOf(4)).add(BigInteger.ONE);
//			BigInteger ysum = y[i][0].multiply(y[i][0]).add(y[i][1].multiply(y[i][1])).add(y[i][2].multiply(y[i][2]));
//			System.out.println(xx.subtract(ysum));
//		}

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


		BigInteger[] f_star = new BigInteger[N];
		for(int i=0;i<N;i++) {
			BigInteger term1 = BigInteger.valueOf(4).multiply(z_x[i]).multiply(gamma_star).multiply(B);
			BigInteger term2 = BigInteger.valueOf(4).multiply(z_x[i]).multiply(z_x[i]);
			BigInteger term3 = gamma_star.multiply(gamma_star);
			BigInteger term4a = z_y[i][0].multiply(z_y[i][0]);
			BigInteger term4b = z_y[i][1].multiply(z_y[i][1]);
			BigInteger term4c = z_y[i][2].multiply(z_y[i][2]);

			f_star[i] = term2.add(term4a).add(term4b).add(term4c).subtract(term1.add(term3));
		}

		// line 10: compute F_star
		BigInteger sum_fstar = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_fstar = sum_fstar.add(f_star[i]);
		}

		ECPoint F_star_term1 = pc.commit(sum_fstar, t_star).decompress();
		ECPoint F_star_term2 = C_star.decompress().multiply(Utils.scalar(gamma_star));
		ECPoint F_star = F_star_term1.subtract(F_star_term2);

		System.out.println(F_star.equals(D_star.decompress()));

		System.out.println("-----");


		return new SharpProof(C_y, D_x, D_y, C_star, D_star, t_x, t_y, t_star, zeta, z_x, z_y, tau, d);
	}
}
