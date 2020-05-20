package com.github.silver_mixer.MCSM;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class MCSM extends JFrame{
	public static final String MCSM_TITLE = "MCSM";
	public static final String MCSM_ERROR_TITLE = "MCSM - エラー";
	public static final String MCSM_CAUTION_TITLE = "MCSM - 警告";
	private static final long serialVersionUID = 1L;
	private static final int PORT = 7327;
	private static final String MCSM_PREFIX = "[MCSM INFO]: ";
	private static String classPath = "";
	private static MCSM mcsm;
	private static JClosableTabbedPane tab;
	private static ArrayList<String> servers = new ArrayList<String>();
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
		
		ServerNode root = new ServerNode("root");
		
		/*
			display.datが存在すればrootに読み込み
			rootを列挙し存在しないファイル名の項目を削除
			serversディレクトリを列挙しrootに存在しないファイルをASCIIソートして追加
			ASCIIソートは追加されたファイルのみで既存項目はソートしない
			(リスト=既存項目+ASCIIソートされた追加ファイル)
		*/

		File displayFile = new File("display.dat");
		if(displayFile.exists()) {
			try {
				FileInputStream fis = new FileInputStream(displayFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				root = (ServerNode)ois.readObject();
				ois.close();
				fis.close();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}else {
			System.out.println(MCSM_PREFIX + "display.dat not found.");
		}
		
		removeFileNotExistServerNode("servers", root);
		addServerNodeFromExistFile(serversFolder, root);
		
		JTree tree = new JTree(root);
		tree.setRootVisible(true);
		tree.setRowHeight(23);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		((DefaultTreeModel)tree.getModel()).setAsksAllowsChildren(true);

		saveServerListDisplay(tree);
		//printServerNode(getAllElements(tree.getPathForRow(0), "root"), 0);
		
		JScrollPane serverListScroll = new JScrollPane(tree);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		layout.setConstraints(serverListScroll, gbc);
		
		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if(event.isShiftDown() && (event.getKeyCode() == KeyEvent.VK_UP || event.getKeyCode() == KeyEvent.VK_DOWN)) {
					TreePath selectPath = tree.getSelectionPath();
					if(selectPath == null || selectPath.getParentPath() == null)return;
					int row = tree.getMinSelectionRow();
					int fromIndex = ((DefaultMutableTreeNode)selectPath.getParentPath().getLastPathComponent()).getIndex((TreeNode)selectPath.getLastPathComponent());
					MutableTreeNode fromNode = (MutableTreeNode)selectPath.getLastPathComponent();
					int deltaIndex = 0;
					if(event.getKeyCode() == KeyEvent.VK_UP) {
						deltaIndex = -1;
					}else if(event.getKeyCode() == KeyEvent.VK_DOWN) {
						deltaIndex = 1;
					}
					if(0 <= (fromIndex + deltaIndex) && (fromIndex + deltaIndex) < ((TreeNode)selectPath.getParentPath().getLastPathComponent()).getChildCount()) {
						((DefaultTreeModel)tree.getModel()).removeNodeFromParent(fromNode);
						((DefaultTreeModel)tree.getModel()).insertNodeInto(fromNode, (DefaultMutableTreeNode)selectPath.getParentPath().getLastPathComponent(), fromIndex + deltaIndex);
						tree.setSelectionRow(tree.getRowForPath(selectPath) + deltaIndex * -1);
						saveServerListDisplay(tree);
					}else {
						tree.setSelectionRow(row + deltaIndex * -1);
					}
				}
			}
		});
		
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if(event.getClickCount() == 2) {
					if(!tree.isSelectionEmpty() && ((DefaultMutableTreeNode)tree.getSelectionPath().getLastPathComponent()).isLeaf()) {
						StringBuilder sb = new StringBuilder();
						for(int i = 1; i < tree.getSelectionPath().getPathCount(); i++) {
							sb.append(tree.getSelectionPath().getPathComponent(i));
							if(i + 1 != tree.getSelectionPath().getPathCount()) {
								sb.append("/");
							}
						}
						String profilePath = sb.toString() + ".dat";
						if(!Server.existLaunchServer(profilePath))new Server(tab, profilePath);
					}
				}
			}
		});
		
		mainPanel.add(menuPanel);
		mainPanel.add(serverListScroll);

		JButton openButton = new JButton("開く");
		openButton.setMaximumSize(new Dimension(openButton.getMaximumSize().width, Integer.MAX_VALUE));
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(!tree.isSelectionEmpty() && ((DefaultMutableTreeNode)tree.getSelectionPath().getLastPathComponent()).isLeaf()) {
					String profilePath = getTreePathToPath(tree.getSelectionPath(), false) + ".dat";
					if(!Server.existLaunchServer(profilePath))new Server(tab, profilePath);
				}
			}
		});

		JButton newButton = new JButton("新規");
		newButton.setMaximumSize(new Dimension(newButton.getMaximumSize().width, Integer.MAX_VALUE));
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(tree.getSelectionPath() == null)return;
				TreePath selectPath = tree.getSelectionPath();
				if(!((TreeNode)selectPath.getLastPathComponent()).getAllowsChildren())return;
				String addName = JOptionPane.showInputDialog(MCSM.getMCSM(), "サーバープロファイル名またはディレクトリ名を入力してください。\nディレクトリ名の場合は最後に「/」を入力して下さい。(例: 「folder/」)\n使用できる文字は半角英数, \"-\", \"_\"です。", MCSM_TITLE, JOptionPane.PLAIN_MESSAGE);
				if(addName != null && !addName.isEmpty()) {
					boolean isFolder = addName.matches("[a-zA-Z0-9-_]+/$");
					boolean isLeef = addName.matches("[a-zA-Z0-9-_]+");
					if(!isFolder && !isLeef) {
						JOptionPane.showMessageDialog(MCSM.getMCSM(), "サーバープロファイル名、ディレクトリ名は半角英数, \"-\", \"_\"で入力してください。", MCSM_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
						return;
					}
					DefaultMutableTreeNode addNode = new DefaultMutableTreeNode(addName.replaceAll("/", ""));
					addNode.setAllowsChildren(isFolder);
					((DefaultTreeModel)tree.getModel()).insertNodeInto(addNode, (DefaultMutableTreeNode)selectPath.getLastPathComponent(), ((TreeNode)selectPath.getLastPathComponent()).getChildCount());
					saveServerListDisplay(tree);
					
					if(isLeef) {
						try {
							File directory = new File(getTreePathToPath(selectPath, true));
							directory.mkdirs();
							File file = new File(directory, addName + ".dat");
							if(file.exists()) {
								JOptionPane.showMessageDialog(MCSM.getMCSM(), "入力されたサーバープロファイル名は既に存在します。", MCSM_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
							}else {
								file.createNewFile();
								new Server(tab, getTreePathToPath(selectPath, false) + addName + ".dat");
							}
						}catch(IOException e) {
							JOptionPane.showMessageDialog(MCSM.getMCSM(), "サーバープロファイルの作成に失敗しました。", MCSM_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
						}
					}else {
						File directory = new File(getTreePathToPath(selectPath, true), addName);
						System.out.println(directory.getAbsolutePath());
						if(directory.exists()) {
							JOptionPane.showMessageDialog(MCSM.getMCSM(), "入力されたディレクトリ名は既に存在します。", MCSM_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
						}else {
							directory.mkdirs();
						}
					}
				}
			}
		});

		JButton removeButton = new JButton("削除");
		removeButton.setMaximumSize(new Dimension(removeButton.getMaximumSize().width, Integer.MAX_VALUE));
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(!tree.isSelectionEmpty()) {
					boolean isLeaf = ((DefaultMutableTreeNode)tree.getSelectionPath().getLastPathComponent()).isLeaf() && !((DefaultMutableTreeNode)tree.getSelectionPath().getLastPathComponent()).getAllowsChildren();
					String path = getTreePathToPath(tree.getSelectionPath(), false) + (!isLeaf ? "" : ".dat");
					String name = tree.getSelectionPath().getLastPathComponent().toString();
					File file = new File(serversFolder, path);
					if((!isLeaf && ((DefaultMutableTreeNode)tree.getSelectionPath().getLastPathComponent()).getChildCount() > 0) || (file.isDirectory() &&file.list().length > 0)) {
						JOptionPane.showMessageDialog(MCSM.getMCSM(), "空ではないディレクトリは削除できません。", MCSM_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
					}else {
						int answer = JOptionPane.showConfirmDialog(MCSM.getMCSM(), "\"" + name + "\"を削除しますか?\n設定は削除され戻せません。", MCSM_CAUTION_TITLE, JOptionPane.YES_NO_OPTION);
						if(answer == JOptionPane.YES_OPTION) {
							if(isLeaf) {
								Server server = Server.getServer(path);
								if(server != null)server.remove();
								servers.remove(getTreePathToPath(tree.getSelectionPath(), false));
							}
							System.out.println(file.getAbsolutePath());
							file.delete();
							((MutableTreeNode)tree.getSelectionPath().getParentPath().getLastPathComponent()).remove(((DefaultMutableTreeNode)tree.getSelectionPath().getLastPathComponent()));
							tree.updateUI();
							saveServerListDisplay(tree);
						}
					}
				}
			}
		});
		
		menuPanel.add(openButton);
		menuPanel.add(newButton);
		menuPanel.add(removeButton);
		
		tab.addTab("Main", mainPanel);
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
			System.out.println(MCSM_PREFIX + "Transfer arguments to an existing process.");
			System.exit(0);
		}catch(ConnectException e) {
			System.out.println(MCSM_PREFIX + "Double start was not detected.");
		}catch(IOException e){
			e.printStackTrace();
		}
		IPCServer server = new IPCServer(PORT + (isTest ? 1 : 0));
		server.start();
		
		classPath = Paths.get(MCSM.class.getProtectionDomain().getCodeSource().getLocation().getPath()).toFile().getParentFile().getAbsolutePath();
		serversFolder = new File(classPath, "servers");
		serversFolder.mkdirs();
		updateServerList(serversFolder, "");
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
				new Server(tab, name + ".dat").start();
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
		for(Server server: Server.getServers())serverList.add(server.getUniqueName());
		return String.join("\n", serverList);
	}
	
	public static void updateServerList(File dir, String path) {
		if(!dir.isDirectory())return;
		for(File file: dir.listFiles()) {
			if(file.isDirectory()) {
				updateServerList(file, path + file.getName() + "/");
			}else if(file.getName().endsWith(".dat")){
				servers.add(path + file.getName().substring(0, file.getName().lastIndexOf(".dat")));
			}
		}
	}
	
	public void removeFileNotExistServerNode(String path, ServerNode node) {
		for(int i = 0; i < node.size(); i++) {
			if(node.get(i) instanceof ServerNode) {
				File directory = new File(path, node.get(i).toString());
				if(directory.exists()) {
					removeFileNotExistServerNode(directory.getPath(), (ServerNode)node.get(i));
				}else {
					node.remove(i);
					i--;
				}
			}else {
				File file = new File(path, (String)node.get(i) + ".dat");
				if(!file.exists()) {
					node.remove(i);
					i--;
				}
			}
		}
	}
	
	public void saveServerListDisplay(JTree tree) {
		ServerNode root = getAllElements(tree.getPathForRow(0), "root");
		try {
			FileOutputStream fos = new FileOutputStream("display.dat");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(root);
			oos.close();
			fos.close();
			System.out.println(MCSM_PREFIX + "Saved display settings.");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addServerNodeFromExistFile(File directory, ServerNode node) {
		List<ServerNode> directoryList = new ArrayList<ServerNode>();
		List<String> fileList = new ArrayList<String>();
		for(File file: directory.listFiles()) {
			if(file.isFile()) {
				if(file.getName().endsWith(".dat")) {
					String name = file.getName().substring(0, file.getName().lastIndexOf(".dat"));
					if(!node.contains(name))fileList.add(name);
				}
			}else{
				ServerNode childNode = node.getServerNode(file.getName());
				if(childNode == null) {
					childNode = new ServerNode(file.getName());
					addServerNodeFromExistFile(file, childNode);
					directoryList.add(childNode);
				}else {
					addServerNodeFromExistFile(file, childNode);
				}
			}
		}
		Collections.sort(directoryList, new Comparator<ServerNode>() {
			@Override
			public int compare(ServerNode n1, ServerNode n2) {
				return n1.toString().compareTo(n2.toString());
			}
		});
		Collections.sort(fileList);
		for(ServerNode e: directoryList)node.add(e);
		for(String e: fileList)node.add(e);
	}
	
	public ServerNode findServerNode(ServerNode node, String name) {
		Iterator<Object> itr = node.iterator();
		while(itr.hasNext()) {
			Object child = itr.next();
			if(child instanceof ServerNode) {
				if(child.toString().equals(name))return (ServerNode)child;
			}
		}
		return null;
	}
	
	public ServerNode getAllElements(TreePath path, String treeName){
		ServerNode nodes = new ServerNode(treeName);
		TreeNode node = (TreeNode)path.getLastPathComponent();
		for(int i = 0; i < node.getChildCount(); i++) {
			TreeNode childNode = node.getChildAt(i);
			if(childNode.isLeaf()) {
				nodes.add(childNode.toString());
			}else {
				nodes.add(getAllElements(new TreePath(((DefaultMutableTreeNode)childNode).getPath()), childNode.toString()));
			}
		}
		return nodes;
	}
	
	public String getTreePathToPath(TreePath path, boolean withServerFolder) {
		StringBuilder strPath = new StringBuilder(withServerFolder ? "servers/" : "");
		for(int i = 1; i < path.getPathCount(); i++) {
			strPath.append(path.getPathComponent(i).toString());
			if(i + 1 != path.getPathCount() || !((DefaultMutableTreeNode)path.getPathComponent(i)).isLeaf())strPath.append("/");
		}
		return strPath.toString();
	}
	
	public void printServerNode(ServerNode node, int depth) {
		Iterator<Object> itr = node.iterator();
		while(itr.hasNext()) {
			Object child = itr.next();
			if(child instanceof ServerNode) {
				for(int i = 0; i < depth + 1; i++)System.out.print(">");
				System.out.println("[" + child.toString() + "]");
				printServerNode((ServerNode)child, depth + 1);
			}else {
				for(int i = 0; i < depth; i++)System.out.print(">");
				System.out.println(child.toString());
			}
		}
	}
}