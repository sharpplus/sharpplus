package decomp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class SquareDecomp {
	
	static final String PRECOMP_FILE_DIR = System.getProperty("user.dir") + "/precomp/";
	static final String DECOMP2_WRITE_FILE_NAME = "decomps2_temp.txt";
	static final String DECOMP3_WRITE_FILE_NAME = "decomps3_temp.txt";
	static final String PRIME_WRITE_FILE_NAME = "primes_temp.txt";
	static final String PRIME_DECOMP_WRITE_FILE_NAME = "prime_decomps_temp.txt";
	static final String DECOMP3_READ_FILE_NAME = "decomps3.txt";
	static final String DECOMP2_READ_FILE_NAME = "decomps2.txt";
	static final String PRIME_READ_FILE_NAME = "primes.txt";
	static final String PRIME_DECOMP_READ_FILE_NAME = "prime_decomps.txt";
	
	Set<Integer> primes;
	int maxPrime = -1;
	
	Map<Integer, int[]> decomp2s;
	int maxDecomp2 = -1;
	
	boolean isPrecompPrime(int inputNumber) {
		return primes.contains(inputNumber);
	}
	
	// https://medium.com/edureka/prime-number-program-in-java-14fcba07dc9d
	boolean isPrime(int inputNumber) {
		if(maxPrime >= 0 && inputNumber < maxPrime) return isPrecompPrime(inputNumber);
		if(inputNumber <= 1) return false;
		else {
			for (int i = 2; i<= inputNumber/2; i++) {
				if ((inputNumber % i) == 0) return false;
			}
			return true;
		}
	}
	
	public void printAllDecomps(int x) {
		System.out.println("x: "+x);
//		int v = (int) Math.sqrt(t) + 1;
		
		for(int i=0;i<x;i++) {
			for(int j=0;j<=i;j++) {
				int k = x - i * i - j * j;
				if(k <= i) {
					int l = (int) Math.sqrt(k);
					if(l * l == k) System.out.println(i+" "+j+" "+l);
				}
			}
		}
	}
	
	public void printAllDecomps(int x, int B) {
		int t = 4 * x * (B - x) + 1;
		
		System.out.println("x: "+x+", B: "+B+", t: "+t);
//		int v = (int) Math.sqrt(t) + 1;
		
		for(int i=0;i<B;i++) {
			for(int j=0;j<=i;j++) {
				int k = t - i * i - j * j;
				if(k <= i) {
					int l = (int) Math.sqrt(k);
					if(l * l == k) System.out.println(i+" "+j+" "+l);
				}
			}
		}
	}
	
	public int[] decomp3(int x) {
		if(x < 0) return new int[] {-1, -1, -1};
		return decomp3base(4 * x + 1);
	}
	
	public int[] decomp3(int x, int B) {
		if(x < 0 || x > B) return new int[] {-1, -1, -1};
		return decomp3base(4 * x * (B - x) + 1);
	}
	
	public int[] decomp3base(int t) {
		for(int i : decomp2s.keySet()) {
			int k = (int) Math.sqrt(t - i);
			if(k * k == t - i) {
				int[] vals = decomp2s.get(i);
				return new int[] {k, vals[0], vals[1]};
			}
			
		}
		
		return new int[] {-1, -1, -1};
	}
	
	public void precomputePrimes(int n) {
	    PrintWriter printWriter;
		try {
			printWriter = new PrintWriter(new FileWriter(PRECOMP_FILE_DIR + PRIME_WRITE_FILE_NAME));
			printWriter.println(n);
			
		    for(int i=0;i<n+1;i++) {
		    	if(isPrime(i) && i % 4 == 1) printWriter.println(i);
		    }
		    
		    printWriter.close();
		    
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void precomputePrimeDecomps(int n) {
	    PrintWriter printWriter;
		try {
			printWriter = new PrintWriter(new FileWriter(PRECOMP_FILE_DIR + PRIME_DECOMP_WRITE_FILE_NAME));
			printWriter.println(n);
			
		    for(int i : primes) {
		    	for(int j = (int) Math.ceil(Math.sqrt(i));j>=0;j--) {
		    		int k = (int) Math.ceil(Math.sqrt(i - j*j));
		    		if(i == j * j + k * k) {
		    			printWriter.println(i+","+j+","+k);
		    			break;
		    		}
		    	}
		    }
		    
		    printWriter.close();
		    
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadPrecompPrimes() {
		primes = new TreeSet<Integer>();
		try (BufferedReader br = new BufferedReader(new FileReader(PRECOMP_FILE_DIR + PRIME_READ_FILE_NAME))) {
		    String line;
		    line = br.readLine();
		    int i = Integer.parseInt(line);
		    maxPrime = i;
		    
		    while ((line = br.readLine()) != null) {
		    	i = Integer.parseInt(line);
		    	primes.add(i);
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void checkPrimeDecomps(int n) {
		SquareDecomp decomp = new SquareDecomp();
		decomp.loadPrecompPrimes();
		
		for(int i=0;i<n+1;i++) {
			int j = 4*i+1;
			boolean success = false;
//			String s = "";
			for(int y=(int) Math.ceil(Math.sqrt(j));y>=0;y--) {
//				if(y % 2 == 1) y--;
				int z = j - y*y;
				if(z >= 0) {
	//				System.out.println(z+" + " + (y*y));
//					s += "check "+z+" + "+(y*y)+"\n";
					if((decomp.primes.contains(z)) || z == 1 || z == 0) success = true;
				}
			}
			if(!success) {
				System.out.println(j+": "+success);
//				System.out.println(s);
			}
		}
	}
	
	public void precomputeDecomp2s(int n) {
	    PrintWriter printWriter;
		try {
			printWriter = new PrintWriter(new FileWriter(PRECOMP_FILE_DIR + DECOMP2_WRITE_FILE_NAME));

		    printWriter.println(n);
		    
		    for(int i=0;i<n+1;i++) {
		    	boolean found = false;
		    	int z = (int) Math.sqrt(i);
		    	for(int j=z;j>=0 && !found;j--) {
		    		for(int k=0;k<=j && !found;k++) {
		    			if(i == j * j + k * k) {
		    				printWriter.println(i+","+j+","+k);
		    				found = true;
		    				System.out.println(i+","+j+","+k);
		    			}
		    		}
		    	}
		    }
		    
		    printWriter.close();
		    
		    
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadPrecompDecomp2s() {
		decomp2s = new TreeMap<Integer, int[]>();
		try (BufferedReader br = new BufferedReader(new FileReader(PRECOMP_FILE_DIR + DECOMP2_READ_FILE_NAME))) {
		    String line;
		    line = br.readLine();
		    int ii = Integer.parseInt(line);
		    maxDecomp2 = ii;
		    
		    while ((line = br.readLine()) != null) {
		    	String[] data = line.split(",");
		    	int i = Integer.parseInt(data[0]);
		    	int j = Integer.parseInt(data[1]);
		    	int k = Integer.parseInt(data[2]);
		    	
		    	decomp2s.put(i, new int[] {j, k});
		    }
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void precomputeDecomp3s(int n) {
	    PrintWriter printWriter;
		try {
			printWriter = new PrintWriter(new FileWriter(PRECOMP_FILE_DIR + DECOMP3_WRITE_FILE_NAME));

		    printWriter.println(n);
		    
		    SquareDecomp decomp = new SquareDecomp();
			decomp.loadPrecompDecomp2s();
			
			long totalBits = 0;
		    
		    for(int i=0;i<n+1;i++) {
		    	int nn = (int) Math.ceil(Math.log(4*i+1)/2);
		    	totalBits += 3*nn;
		    	int[] res = decomp.decomp3(4*i+1);
				printWriter.println(res[0]+","+res[1]+","+res[2]);
				System.out.println(i+": "+res[0]+","+res[1]+","+res[2]);
		    }
		    
		    System.out.println(totalBits/8+" B");
		    
		    printWriter.close();
		    
		    
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] a) {
//		int B = 99;
		
		SquareDecomp decomp = new SquareDecomp();
		decomp.precomputePrimes(1000000);
		decomp.loadPrecompPrimes();
		decomp.precomputePrimeDecomps(1000000);
//		decomp.printAllDecomps(85);
//		
//		decomp.loadPrecompPrimes();
//		decomp.checkPrimeDecomps(250000);
		
//		decomp.printAllDecomps(B);
//		decomp.precomputeDecomp2s(1000000);
		
//		decomp.loadPrecompDecomp2s();
//		decomp.precomputeDecomp3s(1000000);
		
//		for(int i=0;i<110;i++) {
//			int[] res = decomp.decomp3(i, B);
//			System.out.println(i+", "+B+" ("+(4*i*(B-i)+1)+"): "+Arrays.toString(res));
//		}
		
	}

}
