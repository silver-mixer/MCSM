package com.github.silver_mixer.MCSM;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

class IPCServer extends Thread{
	private int port;
	private ServerSocket serverSocket = null;
	
	public IPCServer(int port) {
		this.port = port;
	}
	
	public void run() {
		try {
			serverSocket = new ServerSocket(port);
			while(true) {
				Socket socket = serverSocket.accept();
				IPCClient client = new IPCClient(socket);
				client.start();
			}
		}catch(SocketException e) {
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		try{
			serverSocket.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}