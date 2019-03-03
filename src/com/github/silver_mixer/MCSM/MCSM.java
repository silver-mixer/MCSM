package com.github.silver_mixer.MCSM;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class MCSM extends JFrame{
	private static final long serialVersionUID = 1L;
	private static final int PORT = 7327;
	private static MCSM mcsm;
	private static JClosableTabbedPane tab;
	private static DefaultListModel<String> servers = new DefaultListModel<String>();
	private static File serversFolder, backupsFolder;
	private static final String[] HELP_TEXT = {
			"MCSM - Minecraft Server Manager",
			"usage: [Option]",
			"  -l profile\t: Launch the server.",
			"  -s profile\t: Stop the server when running.",
			"  -h, --help\t: Print this text and exit."
	};

	public static JFrame getMCSM() {
		return mcsm;
	}
	
	public MCSM(){
		setTitle("MCSM");
		setSize(640, 480);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		tab = new JClosableTabbedPane();
		add(tab, BorderLayout.CENTER);
		
		JPanel mainPanel = new JPanel(new GridLayout(2, 1));
		
		GridBagLayout layout = new GridBagLayout();
		mainPanel.setLayout(layout);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		
		JPanel menuPanel = new JPanel();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		layout.setConstraints(menuPanel, gbc);
		menuPanel.setPreferredSize(new Dimension(mainPanel.getPreferredSize().width, 25));
		menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.LINE_AXIS));
		
		JList<String> serverList = new JList<String>(servers);
		JScrollPane serverListScroll = new JScrollPane(serverList);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		layout.setConstraints(serverListScroll, gbc);
		
		mainPanel.add(menuPanel);
		mainPanel.add(serverListScroll);

		JButton openButton = new JButton("開く");
		openButton.setMaximumSize(new Dimension(openButton.getMaximumSize().width, Integer.MAX_VALUE));
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(serverList.getSelectedIndex() != -1) {
					String name = servers.get(serverList.getSelectedIndex());
					if(!Server.existLaunchServer(name))new Server(tab, name);
				}
			}
		});

		JButton newButton = new JButton("新規");
		newButton.setMaximumSize(new Dimension(newButton.getMaximumSize().width, Integer.MAX_VALUE));
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				String addName = JOptionPane.showInputDialog(MCSM.getMCSM(), "サーバープロファイル名を入力してください。", "MCSM", JOptionPane.PLAIN_MESSAGE);
				if(!addName.isEmpty()) {
					if(addName.matches(".*[^a-zA-Z0-9-_].*")) {
						JOptionPane.showMessageDialog(MCSM.getMCSM(), "サーバープロファイル名は半角英数, \"-\", \"_\"で入力してください。", "MCSM - エラー", JOptionPane.ERROR_MESSAGE);
						return;
					}
					for(int i = 0; i < servers.size(); i++) {
						if(addName.equals(servers.get(i))) {
							JOptionPane.showMessageDialog(MCSM.getMCSM(), "既に登録されています。", "MCSM - エラー", JOptionPane.ERROR_MESSAGE);
							return;
						}
					}
					try {
						File file = new File("servers/" + addName + ".dat");
						file.createNewFile();
					}catch(IOException e) {}
					servers.addElement(addName);
					new Server(tab, addName);
				}
			}
		});

		JButton removeButton = new JButton("削除");
		removeButton.setMaximumSize(new Dimension(removeButton.getMaximumSize().width, Integer.MAX_VALUE));
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(serverList.getSelectedIndex() != -1) {
					String name = servers.get(serverList.getSelectedIndex());
					int answer = JOptionPane.showConfirmDialog(MCSM.getMCSM(), "\"" + name + "\"を削除しますか?\n設定は削除され戻せません。", "MCSM - 削除", JOptionPane.YES_NO_OPTION);
					if(answer == JOptionPane.YES_OPTION) {
						Server server = Server.getServer(name);
						if(server != null)server.remove();
						servers.removeElementAt(serverList.getSelectedIndex());
						File file = new File("servers/" + name + ".dat");
						file.delete();
					}
				}
			}
		});
		
		menuPanel.add(openButton);
		menuPanel.add(newButton);
		menuPanel.add(removeButton);
		
		tab.addTab("Main", mainPanel, false);
	}
	
	public static void main(String[] args){
		boolean isTest = false;
		List<String> launchServer = new ArrayList<String>(), stopServer = new ArrayList<String>();
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("--help") || args[i].equals("-h")) {
				for(String line: HELP_TEXT)System.out.println(line);
				System.exit(0);
			}else if(args[i].equals("-l") && i + 1 < args.length) {
				launchServer.add(args[i + 1]);
			}else if(args[i].equals("-s") && i + 1 < args.length) {
				stopServer.add(args[i + 1]);
			}else if(args[i].equals("--test")) {
				isTest = true;
			}
		}
		try{
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress("127.0.0.1", PORT + (isTest ? 1 : 0)), 1000);
			PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
			if(!launchServer.isEmpty())pw.println("launch " + String.join(" ", launchServer));
			if(!stopServer.isEmpty())pw.println("stop " + String.join(" ", stopServer));
			pw.close();
			socket.close();
			System.out.println("[MCSM INFO]: Transfer arguments to an existing process.");
			System.exit(0);
		}catch(ConnectException e) {
			System.out.println("[MCSM INFO]: Double start was not detected.");
		}catch(IOException e){
			e.printStackTrace();
		}
		IPCServer server = new IPCServer(PORT + (isTest ? 1 : 0));
		server.start();
		
		String classPath = Paths.get(MCSM.class.getProtectionDomain().getCodeSource().getLocation().getPath()).toFile().getParentFile().getAbsolutePath();
		serversFolder = new File(classPath, "servers");
		serversFolder.mkdirs();
		for(File file: serversFolder.listFiles()) {
			if(file.isFile() && file.getName().endsWith(".dat"))servers.addElement(file.getName().substring(0, file.getName().lastIndexOf(".dat")));
		}
		backupsFolder = new File(classPath, "backups");
		backupsFolder.mkdirs();
		
		mcsm = new MCSM();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				server.close();
				for(Server server: Server.getServers())server.close();
			}
		});
		
		mcsm.setVisible(true);
		
		for(String name: launchServer)startServer(name);
	}
	
	public static String startServer(String name) {
		if(servers.contains(name)) {
			Server server = Server.getServer(name);
			if(server == null) {
				new Server(tab, name).start();
			}else{
				server.start();
			}
			return "Server \"" + name + "\" has been launched.";
		}
		return "Server \"" + name + "\" does not exist.";
	}
	
	public static String stopServer(String name) {
		Server server = Server.getServer(name);
		if(server != null) {
			server.close();
			return "Server \"" + name + "\" was closed.";
		}
		return "Server was not running.";
	}
	
	public static boolean existServer(String name) {
		return servers.contains(name);
	}
	
	public static File getBackupsFolder() {
		return backupsFolder;
	}
	
	public static File getServersFolder() {
		return serversFolder;
	}
	
	public static String getServersList() {
		List<String> serverList = new ArrayList<String>();
		for(int i = 0; i < servers.size(); i++)serverList.add(servers.get(i));
		return String.join("\n", serverList);
	}
	
	public static String getLaunchServersList() {
		List<String> serverList = new ArrayList<String>();
		for(Server server: Server.getServers())serverList.add(server.getName());
		return String.join("\n", serverList);
	}
}