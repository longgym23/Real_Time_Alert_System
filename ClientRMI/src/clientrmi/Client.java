package clientrmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import share.classes.PhanSo;
import share.interfaces.ITinhToan;

public class Client {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	//ket noi den serverRMI
		try {
			ITinhToan tt = (ITinhToan) Naming.lookup("rmi://localhost:1001/tinhtoan");
			PhanSo ps1 = new PhanSo(1, 2);
			PhanSo ps2 = new PhanSo(1, 3);
			PhanSo ketqua = tt.tinhTong(ps1, ps2);
			System.out.println("Ket qua: " + ketqua.tuSo + "/" + ketqua.mauSo);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
