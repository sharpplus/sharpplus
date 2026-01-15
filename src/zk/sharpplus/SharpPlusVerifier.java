package zk.sharpplus;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import ec.ECPoint;
import util.SharpUtils;
import zk.bulletproofs.PedersenCommitment;
import zk.bulletproofs.Utils;

public class SharpPlusVerifier {
	
	Random generator;
	
	PedersenCommitment pc;
	
	public SharpPlusVerifier() throws NoSuchAlgorithmException {
		pc = PedersenCommitment.getDefault();
		generator = new Random();
	}
	
	public boolean verify(SharpPlusProof p, ECPoint[] C_x, ECPoint[] C_x_prime_1, ECPoint[] C_x_prime_2, BigInteger L) {
		int N = C_x.length;
		int M = C_x_prime_1.length;
		int R = p.tau.length;
		
		// line 2-3 (verifier): verify the shortness of the zetas
		
		for(int i=0;i<p.zeta.length;i++) {
			if(p.zeta[i].compareTo(BigInteger.valueOf(4).multiply(BigInteger.valueOf(N)).multiply(L)) > 0) return false;
		}
		
		byte[] challenge1 = SharpUtils.fiatShamir(C_x, p.C_y, p.C_y_prime, N, R);
		boolean[] gammas1 = SharpUtils.drawBooleansFromSeed(challenge1, N * 4 * R);
		
		boolean[][][] gamma = new boolean[N][3][R];
		
		for(int i=0;i<N;i++) {
			for(int j=0;j<3;j++) {
				for(int k=0;k<R;k++) {
					gamma[i][j][k] = gammas1[3 * R * i + R * j + k];
				}
			}
		}
		
		byte[] challenge2 = SharpUtils.fiatShamir(C_x, p.C_y, p.C_y_prime, N, R);
		BigInteger gamma_star = SharpUtils.drawNumberFromSeed(challenge2);
		
		// line 5 (verifier): compute F_x
		
		BigInteger sum_z_x = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_z_x = sum_z_x.add(p.z_x[i].multiply(gamma_star.modPow(BigInteger.valueOf(i),PedersenCommitment.GROUP_ORDER)));
		}
		for(int i=0;i<M;i++) {
			sum_z_x = sum_z_x.add(gamma_star.modPow(BigInteger.valueOf(N+i),PedersenCommitment.GROUP_ORDER).multiply(p.z_x_prime_1[i]));
			sum_z_x = sum_z_x.add(gamma_star.modPow(BigInteger.valueOf(N+M+i),PedersenCommitment.GROUP_ORDER).multiply(p.z_x_prime_2[i]));
		}
		
		ECPoint F_x = pc.commit(sum_z_x, p.t_x).decompress();
		for(int i=0;i<C_x.length;i++) {
			ECPoint F_x_term = C_x[i].decompress().multiply(Utils.scalar(gamma_star.modPow(BigInteger.valueOf(2+i),PedersenCommitment.GROUP_ORDER)));
			F_x = F_x.subtract(F_x_term);
		}
		for(int i=0;i<C_x_prime_1.length;i++) {
			ECPoint F_x_term2 = C_x_prime_1[i].decompress().multiply(Utils.scalar(gamma_star.modPow(BigInteger.valueOf(N+i+1),PedersenCommitment.GROUP_ORDER)));
			ECPoint F_x_term3 = C_x_prime_2[i].decompress().multiply(Utils.scalar(gamma_star.modPow(BigInteger.valueOf(N+M+i+1),PedersenCommitment.GROUP_ORDER)));
			F_x = F_x.subtract(F_x_term2).subtract(F_x_term3);
		}
		
		ECPoint D_x = p.D_x[0].decompress();
		for(int i=1;i<p.D_x.length;i++) {
			ECPoint F_x_term = p.D_x[i].decompress().multiply(Utils.scalar(gamma_star.modPow(BigInteger.valueOf(i),PedersenCommitment.GROUP_ORDER)));
			D_x = D_x.add(F_x_term);
		}
		for(int i=0;i<C_x_prime_1.length;i++) {
			ECPoint F_x_term2 = p.D_x_prime_1[i].decompress().multiply(Utils.scalar(gamma_star.modPow(BigInteger.valueOf(N+i),PedersenCommitment.GROUP_ORDER)));
			ECPoint F_x_term3 = p.D_x_prime_2[i].decompress().multiply(Utils.scalar(gamma_star.modPow(BigInteger.valueOf(N+M+i),PedersenCommitment.GROUP_ORDER)));
			D_x = D_x.add(F_x_term2).add(F_x_term3);
		}
		
//		System.out.println(F_x.equals(D_x.decompress()));
		if (!F_x.equals(D_x)) return false;
		
		// line 6 (verifier): compute F_y
		
		BigInteger sum_z_y_tau = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			for(int j=0;j<3;j++) {
				sum_z_y_tau = sum_z_y_tau.add(p.z_y[i][j]);
			}
		}
		
		for(int i=0;i<M;i++) {
			sum_z_y_tau = sum_z_y_tau.add(p.z_y_prime[i]);
		}
		
		for(int k=0;k<R;k++) {
			sum_z_y_tau = sum_z_y_tau.add(p.tau[k]);
		}
		
		ECPoint F_y_term1 = pc.commit(sum_z_y_tau, p.t_y).decompress();
		ECPoint F_y_term2 = p.C_y.decompress().multiply(Utils.scalar(gamma_star));
		ECPoint F_y_term3 = p.C_y_prime.decompress().multiply(Utils.scalar(gamma_star)).multiply(Utils.scalar(gamma_star));
		ECPoint F_y = F_y_term1.subtract(F_y_term2).subtract(F_y_term3);
		
//		System.out.println(F_y.equals(p.D_y.decompress()));
		if(!F_y.equals(p.D_y.decompress())) return false;
		
		// line 7, 8: compute f
		
		
		BigInteger[] f = new BigInteger[R];
		for(int k=0;k<R;k++) {
			f[k] = p.tau[k];
			for(int i=0;i<N;i++) {
				for(int j=0;j<3;j++) {
					if(gamma[i][j][k]) {
						f[k] = f[k].add(p.z_y[i][j]);
					}
				}
			}
			f[k] = f[k].subtract(gamma_star.multiply(p.zeta[k]));
			
			
//			System.out.println("k="+k+": "+f[k].equals(p.d[k]));
			if(!f[k].equals(p.d[k])) return false;
		}
		
		// line 9: compute f^star
		
		BigInteger[] f_star = new BigInteger[N];
		for(int i=0;i<N;i++) {
			f_star[i] = BigInteger.valueOf(4).multiply(p.z_x[i]).add(gamma_star.multiply(gamma_star)).subtract(p.z_y[i][0].multiply(p.z_y[i][0]).add(p.z_y[i][1].multiply(p.z_y[i][1])).add(p.z_y[i][2].multiply(p.z_y[i][2])));
		}		
		BigInteger[] f_star_prime = new BigInteger[M];
		for(int i=0;i<M;i++) {
			f_star_prime[i] = p.z_y_prime[i].subtract(p.z_x_prime_1[i].multiply(p.z_x_prime_2[i]));
		}
		
		// line 10: compute F_star
		BigInteger sum_fstar = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_fstar = sum_fstar.add(f_star[i].multiply(gamma_star.modPow(BigInteger.valueOf(i),PedersenCommitment.GROUP_ORDER)));
		}
		for(int i=0;i<M;i++) {
			sum_fstar = sum_fstar.add(f_star_prime[i].multiply(gamma_star.modPow(BigInteger.valueOf(N+i),PedersenCommitment.GROUP_ORDER)));
		}
		
		ECPoint F_star = pc.commit(sum_fstar, p.t_star).decompress();
		for(int i=0;i<p.C_star.length;i++) {
			ECPoint C_star_term = p.C_star[i].decompress().multiply(Utils.scalar(gamma_star.modPow(BigInteger.valueOf(i+1),PedersenCommitment.GROUP_ORDER)));
			F_star = F_star.subtract(C_star_term);
		}
		for(int i=0;i<p.C_star_prime.length;i++) {
			ECPoint F_x_term2 = p.C_star_prime[i].decompress().multiply(Utils.scalar(gamma_star.modPow(BigInteger.valueOf(N+i+1),PedersenCommitment.GROUP_ORDER)));
			F_star = F_star.subtract(F_x_term2);
		}
		
		ECPoint D_star = p.D_star[0].decompress();
		for(int i=1;i<p.D_star.length;i++) {
			ECPoint D_star_term = p.D_star[i].decompress().multiply(Utils.scalar(gamma_star.modPow(BigInteger.valueOf(i),PedersenCommitment.GROUP_ORDER)));
			D_star = D_star.add(D_star_term);
		}
		for(int i=0;i<p.D_star_prime.length;i++) {
			ECPoint F_x_term2 = p.D_star_prime[i].decompress().multiply(Utils.scalar(gamma_star.modPow(BigInteger.valueOf(N+i),PedersenCommitment.GROUP_ORDER)));
			D_star = D_star.add(F_x_term2);
		}
		
//		System.out.println(F_star.equals(p.D_star.decompress()));
		if(!F_star.equals(D_star)) return false;
		
		return true;
	}

}
