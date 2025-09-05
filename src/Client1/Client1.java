package Client1;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import share.phanso.phanso;
import share.phansoss.ITinhtoan;

public class Client1 {
	public static void main(String[] args) {
		try {
			ITinhtoan tt = (ITinhtoan) Naming.lookup("rmi://localhost:1003/tinhtong");
			phanso ps1 = new phanso(1, 2);
			phanso ps2 = new phanso(1, 3);
			phanso ketqua = tt.TinhTong(ps1, ps2);
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
