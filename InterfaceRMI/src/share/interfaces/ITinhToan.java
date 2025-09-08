package share.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

import share.classes.PhanSo;

public interface ITinhToan extends Remote{
	public PhanSo tinhTong(PhanSo ps1, PhanSo ps2) throws RemoteException;
	
}
