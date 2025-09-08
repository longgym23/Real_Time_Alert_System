package rmiserver;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import share.classes.PhanSo;
import share.interfaces.ITinhToan;

public class TinhToan extends UnicastRemoteObject implements ITinhToan{
	public TinhToan() throws RemoteException {
		super();
	}

@Override
public PhanSo tinhTong(PhanSo ps1, PhanSo ps2) throws RemoteException {
	PhanSo ketqua = new PhanSo();
	ketqua.tuSo = (ps1.tuSo * ps2.mauSo) + (ps2.tuSo * ps1.mauSo);
	ketqua.mauSo = ps1.mauSo * ps2.mauSo;
	return ketqua;
}
}