package zk.sharpplus;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;

import ec.ECPoint;

public class SharpPlusProof implements Serializable {
	
	static final long serialVersionUID = 1L;

	ECPoint C_y;
	ECPoint C_y_prime;
	ECPoint[] D_x;
	ECPoint[] D_x_prime_1;
	ECPoint[] D_x_prime_2;
	ECPoint D_y;
	ECPoint[] C_star;
	ECPoint[] D_star;
	ECPoint[] C_star_prime;
	ECPoint[] D_star_prime;
	BigInteger t_x;
	BigInteger t_y;
	BigInteger t_star;
	BigInteger[] zeta;
	BigInteger[] z_x;
	BigInteger[][] z_y;
	BigInteger[] z_y_prime;
	BigInteger[] z_x_prime_1;
	BigInteger[] z_x_prime_2;
	BigInteger[] tau;
	BigInteger[] d;
	
	public SharpPlusProof(ECPoint C_y, ECPoint C_y_prime, ECPoint[] D_x, ECPoint[] D_x_prime_1, ECPoint[] D_x_prime_2, ECPoint D_y, ECPoint[] C_star, ECPoint[] C_star_prime, ECPoint[] D_star, ECPoint[] D_star_prime, 
			BigInteger t_x, BigInteger t_y, BigInteger t_star, BigInteger[] zeta, BigInteger[] z_x, BigInteger[][] z_y, BigInteger[] z_y_prime, BigInteger[] z_x_prime_1, BigInteger[] z_x_prime_2, BigInteger[] tau, BigInteger[] d) {
		this.C_y = C_y;
		this.C_y_prime = C_y_prime;
		this.D_x = D_x;
		this.D_x_prime_1 = D_x_prime_1;
		this.D_x_prime_2 = D_x_prime_2;
		this.D_y = D_y;
		this.C_star = C_star;
		this.D_star = D_star;
		this.C_star_prime = C_star_prime;
		this.D_star_prime = D_star_prime;
		this.t_x = t_x;
		this.t_y = t_y;
		this.t_star = t_star;
		this.zeta = zeta;
		this.z_x = z_x;
		this.z_y = z_y;
		this.z_y_prime = z_y_prime;
		this.z_x_prime_1 = z_x_prime_1;
		this.z_x_prime_2 = z_x_prime_2;
		this.tau = tau;
		this.d = d;
	}
	
	public int size() {
		return this.serialize().length;
	}
	
	public byte[] serialize() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

	    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
	        out.writeObject(this);
	        out.flush();
	        return bos.toByteArray();
	    } catch (Exception ex) {
	        throw new RuntimeException(ex);
	    }	
	}
	
}
