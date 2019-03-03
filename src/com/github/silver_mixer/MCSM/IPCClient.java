package com.github.silver_mixer.MCSM;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class IPCClient extends Thread{
	private Socket socket;
	private PrintWriter pw;
	private boolean hasAllPermission;
	private List<String> permission = new ArrayList<String>();
	
	public IPCClient(Socket socket) {
		this.socket = socket;
		hasAllPermission = socket.getInetAddress().getHostAddress().equals("127.0.0.1");
		permission.add("launch.namako");
		permission.add("stop.namako");
		permission.add("list");
		permission.add("list.-l");
		permission.add("backup.list.namako");
		permission.add("backup.make.namako");
		permission.add("backup.restore.namako.*");
	}
	
	public void run() {
		BufferedReader br;
		try{
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			pw = new PrintWriter(socket.getOutputStream(), true);
		}catch(IOException e){
			e.printStackTrace();
			return;
		}
		System.out.println("[MCSM INFO]: Message Begin <" + socket.getInetAddress().getHostAddress() + ">");
		while(true) {
			try{
				String line = br.readLine();
				if(line == null) {
					System.out.println("[MCSM INFO]: Message End");
					break;
				}
				String[] command = line.split(" ");
				if(command.length == 0)continue;
				System.out.println(String.join(".", command));
				if(hasAllPermission || hasPermission(String.join(".", command))) {
					onCommand(command[0], (command.length == 1 ? new String[0] : Arrays.copyOfRange(command, 1, command.length)));
				}else {
					pw.println("You don't have permission run this command.");
				}
				pw.println("@ok");
				System.out.println("[MCSM INFO]: > " + line);
			}catch(IOException e){
				e.printStackTrace();
				break;
			}
		}
		try{
			br.close();
			socket.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void onCommand(String commandLabel, String[] args) {
		if(commandLabel.equals("launch")) {
			for(int i = 0; i < args.length; i++) {
				pw.println(MCSM.startServer(args[i]));
			}
		}else if(commandLabel.equals("stop")) {
			for(int i = 0; i < args.length; i++) {
				pw.println(MCSM.stopServer(args[i]));
			}
		}else if(commandLabel.equals("list")){
			if(args.length == 0) {
				pw.println("[available server list]");
				pw.println(MCSM.getServersList());
			}else if(args.length == 1 && args[0].equals("-l")) {
				pw.println("[running server list]");
				pw.println(MCSM.getLaunchServersList());
			}else {
				pw.println("Usage: list [-l]");
			}
		}else if(commandLabel.equals("backup")){
			if(args.length == 2) {
				if(args[0].equals("list")) {
					pw.println(Server.getServerBackupList(args[1]));
				}else if(args[0].equals("make")) {
					pw.println("Creating backup. Please wait.");
					pw.println(Server.backupServer(args[1]));
				}
			}else if(args.length == 3) {
				if(args[0].equals("restore")) {
					pw.println("Apply backup. Please wait.");
					pw.println(Server.restoreServer(args[1], args[2]));
				}
			}else {
				pw.println("Usage: backup <list | make | restore> <profile> [backup_id]");
			}
		}else {
			pw.println("Unknown Request.");
		}
	}
	
	public boolean hasPermission(String command) {
		if(permission.contains(command))return true;
		for(String line: permission) {
			if(line.endsWith("*")) {
				String newPermission = line.replaceAll("\\.\\*", ".");
				System.out.println(newPermission);
				if(command.startsWith(newPermission))return true;
			}
		}
		return false;
	}
}