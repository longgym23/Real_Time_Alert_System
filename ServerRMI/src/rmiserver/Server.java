package rmiserver;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Server {
	public static void main(String[] args)  {
		//1 dinh nghia interface : nhung phuong thuc server chia se(Ca Client vaf Server deu phai co Interface )
		//2 Ben Server:
		//	- Dinh nghia doi tuong ke thua tu interface sau do dinh nghia cac phuong thuc chia se hoat dong nhu nao
		//  -Khoi tao doi tuong chua phuong thuc chi ase
		//	- dang ky port chia se
		//	- Public method ra Internet
		//3 Ben Client:
		//	- remote den Public Method tren ServerRMI
		//	- Su dung Public Method
		
		try {
			//khoi tao doi tuong chua public method
			TinhToan tt = new TinhToan();
			//Dang ky port
			LocateRegistry.createRegistry(1001);
			Naming.rebind("rmi://localhost:1001/tinhtoan", tt);
			
		} catch (RemoteException | MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}