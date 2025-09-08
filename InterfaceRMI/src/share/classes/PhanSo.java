package share.classes;

import java.io.Serializable;

public class PhanSo implements Serializable {
	public double tuSo;
	public double mauSo;
	public PhanSo() {
		super();
	}
	public PhanSo(double tuSo, double mauSo) {
		super();
		this.tuSo = tuSo;
		this.mauSo = mauSo;
	}
	
}
