package zk.sharpmultibackup;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;

import ec.ECPoint;

public class SharpMultiProof implements Serializable {
	
	static final long serialVersionUID = 1L;

	ECPoint C_x;
	ECPoint C_y;
	ECPoint D_x;
	ECPoint D_y;
	ECPoint C_star;
	ECPoint D_star;
	BigInteger t_x;
	BigInteger t_y;
	BigInteger t_star;
	BigInteger[] z_x;
	BigInteger[] z_y1;
	BigInteger[] z_y2;
	
	public SharpMultiProof(ECPoint C_x, ECPoint C_y, ECPoint D_x, ECPoint D_y, ECPoint C_star, ECPoint D_star, 
			BigInteger t_x, BigInteger t_y, BigInteger t_star, BigInteger[] z_x, BigInteger[] z_y1, BigInteger[] z_y2) {
		this.C_x = C_x;
		this.C_y = C_y;
		this.D_x = D_x;
		this.D_y = D_y;
		this.C_star = C_star;
		this.D_star = D_star;
		this.t_x = t_x;
		this.t_y = t_y;
		this.t_star = t_star;
		this.z_x = z_x;
		this.z_y1 = z_y1;
		this.z_y2 = z_y2;
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
