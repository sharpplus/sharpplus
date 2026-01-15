package zk.sharp1s;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import ec.ECPoint;
import util.SharpUtils;
import zk.bulletproofs.PedersenCommitment;
import zk.bulletproofs.Utils;

public class Sharp1sVerifier {
	
	Random generator;
	
	PedersenCommitment pc;
	
	public Sharp1sVerifier() throws NoSuchAlgorithmException {
		pc = PedersenCommitment.getDefault();
		generator = new Random();
	}
	
	public boolean verify(Sharp1sProof p, BigInteger L) {
		int N = p.z_x.length;
		int R = p.tau.length;
		
		// line 2-3 (verifier): verify the shortness of the zetas
		
		for(int i=0;i<p.zeta.length;i++) {
			if(p.zeta[i].compareTo(BigInteger.valueOf(4).multiply(BigInteger.valueOf(N)).multiply(L)) > 0) return false;
		}
		
		byte[] challenge1 = SharpUtils.fiatShamir(p.C_x, p.C_y, N, R);
		boolean[] gammas1 = SharpUtils.drawBooleansFromSeed(challenge1, N * 4 * R);
		
		boolean[][][] gamma = new boolean[N][3][R];
		
		for(int i=0;i<N;i++) {
			for(int j=0;j<3;j++) {
				for(int k=0;k<R;k++) {
					gamma[i][j][k] = gammas1[3 * R * i + R * j + k];
				}
			}
		}
		
		byte[] challenge2 = SharpUtils.fiatShamir(p.C_x, p.C_y, N, R);
		BigInteger gamma_star = SharpUtils.drawNumberFromSeed(challenge2);
		
		// line 5 (verifier): compute F_x
		
		BigInteger sum_z_x = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			sum_z_x = sum_z_x.add(p.z_x[i]);
		}
		
		ECPoint F_x_term1 = pc.commit(sum_z_x, p.t_x).decompress();
		ECPoint F_x_term2 = p.C_x.decompress().multiply(Utils.scalar(gamma_star.multiply(gamma_star)));
		ECPoint F_x = F_x_term1.subtract(F_x_term2);
		
//		System.out.println(F_x.equals(p.D_x.decompress()));
		if (!F_x.equals(p.D_x.decompress())) return false;
		
		// line 6 (verifier): compute F_y
		
		BigInteger sum_z_y_tau = BigInteger.ZERO;
		for(int i=0;i<N;i++) {
			for(int j=0;j<3;j++) {
				sum_z_y_tau = sum_z_y_tau.add(p.z_y[i][j]);
			}
		}
		
		for(int k=0;k<R;k++) {
			sum_z_y_tau = sum_z_y_tau.add(p.tau[k]);
		}
		
		ECPoint F_y_term1 = pc.commit(sum_z_y_tau, p.t_y).decompress();
		ECPoint F_y_term2 = p.C_y.decompress().multiply(Utils.scalar(gamma_star));
		ECPoint F_y = F_y_term1.subtract(F_y_term2);
		
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
		
		// line 10: compute F_star
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
